package fr.boisnature.photos;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_FILES = 10, PICK_TREE = 11;
    private static final String[] DEFAULT_ROOMS = {"RANGEMENT","SALLE À MANGER","CHAMBRE","SALON","TERRASSE","BUREAU","SALLE DE BAIN"};
    private final LinkedHashMap<String, ArrayList<String>> photos = new LinkedHashMap<>();
    private final LinkedHashMap<String, ArrayList<String>> trees = new LinkedHashMap<>();
    private final LinkedHashMap<String, ArrayList<String>> excluded = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout tabs;
    private ImageView image;
    private TextView status;
    private String activeRoom;
    private ArrayList<String> visible = new ArrayList<>();
    private int index = 0;
    private boolean playing = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        load();
        activeRoom = photos.keySet().iterator().next();
        buildUi();
        selectRoom(activeRoom);
    }

    private TextView button(String text, int color, View.OnClickListener listener) {
        TextView b = new TextView(this);
        b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setGravity(Gravity.CENTER);
        b.setPadding(22,14,22,14); b.setBackgroundColor(color); b.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,-1); p.setMargins(5,5,5,5); b.setLayoutParams(p);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(74,47,28));
        HorizontalScrollView scroller = new HorizontalScrollView(this); scroller.setFillViewport(true);
        tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(8,8,8,8); scroller.addView(tabs);
        root.addView(scroller, new LinearLayout.LayoutParams(-1,82));
        image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setBackgroundColor(Color.rgb(15,31,28));
        image.setOnClickListener(v -> next());
        root.addView(image, new LinearLayout.LayoutParams(-1,0,1));
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setGravity(Gravity.CENTER); status.setPadding(8,4,8,4);
        root.addView(status, new LinearLayout.LayoutParams(-1,42));
        LinearLayout tools = new LinearLayout(this); tools.setGravity(Gravity.CENTER); tools.setBackgroundColor(Color.rgb(122,75,38));
        tools.addView(button("‹",0xFF17A9A0,v -> previous()));
        tools.addView(button("▶ / ⏸",0xFFFF6B4A,v -> togglePlay()));
        tools.addView(button("›",0xFF17A9A0,v -> next()));
        tools.addView(button("＋ PHOTOS",0xFF3B7A3E,v -> pickFiles()));
        tools.addView(button("＋ DOSSIER",0xFF3B7A3E,v -> pickTree()));
        tools.addView(button("− PHOTO",0xFFB23A24,v -> removeCurrent()));
        root.addView(tools, new LinearLayout.LayoutParams(-1,64));
        setContentView(root);
        renderTabs();
    }

    private void renderTabs() {
        tabs.removeAllViews();
        for (String room : photos.keySet()) {
            int color = room.equals(activeRoom) ? 0xFFFFC84B : 0xFF7A4B26;
            TextView b = button(room, color, v -> selectRoom(room));
            b.setTextColor(room.equals(activeRoom) ? 0xFF2B1B10 : Color.WHITE);
            tabs.addView(b);
        }
        tabs.addView(button("＋ PIÈCE",0xFF17A9A0,v -> addRoom()));
    }

    private void selectRoom(String room) {
        activeRoom = room; index = 0; stopPlay(); renderTabs(); refresh();
    }

    private void refresh() {
        visible = new ArrayList<>(photos.get(activeRoom));
        for (String tree : trees.get(activeRoom)) scanTree(Uri.parse(tree), visible);
        LinkedHashSet<String> unique = new LinkedHashSet<>(visible); visible = new ArrayList<>(unique);
        visible.removeAll(excluded.get(activeRoom));
        show();
    }

    private void show() {
        if (visible.isEmpty()) { image.setImageDrawable(null); status.setText(activeRoom + " — Ajoutez des photos ou un dossier"); return; }
        index = (index % visible.size() + visible.size()) % visible.size();
        Uri uri = Uri.parse(visible.get(index));
        try { image.setImageURI(null); image.setImageURI(uri); status.setText(activeRoom + "  •  " + (index+1) + " / " + visible.size()); }
        catch (Exception e) { image.setImageDrawable(null); status.setText("Image illisible : " + e.getMessage()); }
    }

    private void next() { if (!visible.isEmpty()) { index++; show(); } }
    private void previous() { if (!visible.isEmpty()) { index--; show(); } }
    private final Runnable advance = new Runnable() { public void run() { if (playing) { next(); handler.postDelayed(this,4000); } } };
    private void togglePlay() { if (playing) stopPlay(); else { playing=true; handler.postDelayed(advance,4000); } }
    private void stopPlay() { playing=false; handler.removeCallbacks(advance); }

    private void pickFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_FILES);
    }
    private void pickTree() {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),PICK_TREE);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data); if (result != RESULT_OK || data == null) return;
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (request == PICK_FILES) {
            if (data.getClipData()!=null) for(int n=0;n<data.getClipData().getItemCount();n++) addUri(data.getClipData().getItemAt(n).getUri(),flags);
            else if(data.getData()!=null) addUri(data.getData(),flags);
        } else if (request == PICK_TREE && data.getData()!=null) {
            Uri uri=data.getData(); try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored) {}
            if(!trees.get(activeRoom).contains(uri.toString())) trees.get(activeRoom).add(uri.toString());
        }
        save(); refresh();
    }

    private void addUri(Uri uri, int flags) {
        try { getContentResolver().takePersistableUriPermission(uri,flags & Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored) {}
        if(!photos.get(activeRoom).contains(uri.toString())) photos.get(activeRoom).add(uri.toString());
        excluded.get(activeRoom).remove(uri.toString());
    }

    private void scanTree(Uri tree, ArrayList<String> out) {
        try {
            String parent=DocumentsContract.getTreeDocumentId(tree);
            scanDocument(tree,parent,out);
        } catch(Exception ignored) {}
    }
    private void scanDocument(Uri tree, String parent, ArrayList<String> out) {
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
        try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)) {
            if(c==null)return;
            while(c.moveToNext()) {
                String id=c.getString(0), mime=c.getString(1);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) scanDocument(tree,id,out);
                else if(mime!=null && mime.startsWith("image/")) out.add(DocumentsContract.buildDocumentUriUsingTree(tree,id).toString());
            }
        } catch(Exception ignored) {}
    }

    private void removeCurrent() {
        if(visible.isEmpty()) return;
        String uri=visible.get(index);
        photos.get(activeRoom).remove(uri);
        if(!excluded.get(activeRoom).contains(uri)) excluded.get(activeRoom).add(uri);
        save(); refresh();
    }

    private void addRoom() {
        final EditText input=new EditText(this); input.setHint("Nom de la pièce");
        new AlertDialog.Builder(this).setTitle("Nouvelle pièce").setView(input).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",(d,w)->{
            String name=input.getText().toString().trim().toUpperCase(Locale.FRENCH);
            if(!name.isEmpty()&&!photos.containsKey(name)){photos.put(name,new ArrayList<>());trees.put(name,new ArrayList<>());excluded.put(name,new ArrayList<>());save();selectRoom(name);}
        }).show();
    }

    private void load() {
        try {
            JSONObject root=new JSONObject(getPreferences(MODE_PRIVATE).getString("data","{}"));
            JSONArray roomNames=root.optJSONArray("rooms");
            if(roomNames!=null) for(int i=0;i<roomNames.length();i++) loadRoom(root,roomNames.getString(i));
        } catch(Exception ignored) {}
        if(photos.isEmpty()) for(String r:DEFAULT_ROOMS){photos.put(r,new ArrayList<>());trees.put(r,new ArrayList<>());excluded.put(r,new ArrayList<>());}
    }
    private void loadRoom(JSONObject root,String room)throws JSONException{
        photos.put(room,jsonList(root.optJSONArray("p_"+room))); trees.put(room,jsonList(root.optJSONArray("t_"+room))); excluded.put(room,jsonList(root.optJSONArray("x_"+room)));
    }
    private ArrayList<String> jsonList(JSONArray a)throws JSONException{
        ArrayList<String> list=new ArrayList<>(); if(a!=null)for(int i=0;i<a.length();i++)list.add(a.getString(i)); return list;
    }
    private void save() {
        try {
            JSONObject root=new JSONObject(); root.put("rooms",new JSONArray(photos.keySet()));
            for(String room:photos.keySet()){root.put("p_"+room,new JSONArray(photos.get(room)));root.put("t_"+room,new JSONArray(trees.get(room)));root.put("x_"+room,new JSONArray(excluded.get(room)));}
            getPreferences(MODE_PRIVATE).edit().putString("data",root.toString()).apply();
        } catch(Exception ignored) {}
    }
}
