package com.fabriccolourmaster;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.content.ContentValues;
import android.database.sqlite.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PICK_SAVE=101, PICK_MATCH=102;
    DB db; ImageView preview, matchPreview; EditText name, quality, roll, notes, search;
    TextView sampled, matches, library; byte[] currentImage, matchImage; int[] currentRGB, matchRGB;
    LinearLayout libraryBox, matchBox; Bitmap currentBitmap, matchBitmap;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); db=new DB(this); buildUI(); refreshLibrary("");
    }
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(35,45,55));t.setPadding(0,dp(5),0,dp(5));return t;}
    Button button(String s, Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
    EditText edit(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);return e;}
    void buildUI(){
        ScrollView scroll=new ScrollView(this); LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(16),dp(14),dp(16),dp(24));page.setBackgroundColor(Color.rgb(244,246,248));scroll.addView(page);setContentView(scroll);
        TextView title=text("🎨 Fabric Colour Master",24);title.setTypeface(null,1);page.addView(title);page.addView(text("Offline fabric shade library",14));
        card(page,"1. Save a colour");
        page.addView(button("Take / choose fabric photo",()->pick(PICK_SAVE)));
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setMaxHeight(dp(190));page.addView(preview);
        sampled=text("No photo selected");page.addView(sampled);
        name=edit("Colour name (required) e.g. Mid Brown");page.addView(name);
        quality=edit("Fabric quality");page.addView(quality);
        roll=edit("Roll / lot number");page.addView(roll);
        notes=edit("Notes");page.addView(notes);
        page.addView(button("SAVE COLOUR",()->save()));
        card(page,"2. Find closest saved colour");
        page.addView(button("Take / choose photo to match",()->pick(PICK_MATCH)));
        matchPreview=new ImageView(this);matchPreview.setAdjustViewBounds(true);matchPreview.setMaxHeight(dp(160));page.addView(matchPreview);
        page.addView(button("FIND CLOSEST MATCHES",()->findMatches()));
        matches=text("Choose a photo, then tap Find Closest Matches.");page.addView(matches);
        card(page,"3. My saved colours");
        search=edit("Search name, quality or roll number");page.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){refreshLibrary(s.toString());}public void afterTextChanged(Editable e){}});
        libraryBox=new LinearLayout(this);libraryBox.setOrientation(1);page.addView(libraryBox);
        page.addView(button("EXPORT BACKUP",()->exportBackup()));
        page.addView(text("Photos and records are stored on this phone. Export backups regularly. Camera lighting can affect colour matching; compare under similar light.",12));
    }
    void card(LinearLayout parent,String title){TextView t=text(title,18);t.setTypeface(null,1);t.setPadding(0,dp(16),0,dp(8));parent.addView(t);}
    void pick(int request){Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(Intent.createChooser(i,"Choose fabric photo"),request);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{
        Uri u=data.getData();InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);byte[] bytes=out.toByteArray();Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bmp==null){toast("Could not read photo.");return;}
        int[] rgb=sample(bmp);
        if(req==PICK_SAVE){currentImage=bytes;currentBitmap=bmp;currentRGB=rgb;preview.setImageBitmap(bmp);sampled.setText("Estimated colour: "+hex(rgb)+"   RGB "+rgb[0]+", "+rgb[1]+", "+rgb[2]);}
        else{matchImage=bytes;matchBitmap=bmp;matchRGB=rgb;matchPreview.setImageBitmap(bmp);matches.setText("Photo sample: "+hex(rgb)+" — tap Find Closest Matches.");}
    }catch(Exception e){toast("Could not open photo.");}}
    int[] sample(Bitmap original){
        Bitmap b=Bitmap.createScaledBitmap(original,50,50,true);long r=0,g=0,bl=0,count=0;
        for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){int c=b.getPixel(x,y);int rr=Color.red(c),gg=Color.green(c),bb=Color.blue(c);if(rr>248&&gg>238&&bb>238)continue;r+=rr;g+=gg;bl+=bb;count++;}
        if(count==0)count=1;return new int[]{(int)(r/count),(int)(g/count),(int)(bl/count)};
    }
    String hex(int[] c){return String.format("#%02X%02X%02X",c[0],c[1],c[2]);}
    void save(){String n=name.getText().toString().trim();if(currentImage==null||currentRGB==null||n.isEmpty()){toast("Take a photo and enter a colour name first.");return;}
        db.add(n,quality.getText().toString().trim(),roll.getText().toString().trim(),notes.getText().toString().trim(),currentImage,currentRGB);
        name.setText("");quality.setText("");roll.setText("");notes.setText("");preview.setImageDrawable(null);sampled.setText("Saved successfully.");currentImage=null;currentRGB=null;currentBitmap=null;refreshLibrary(search==null?"":search.getText().toString());toast("Colour saved on this phone.");}
    double dist(int[] a,int[] b){double s=0;for(int i=0;i<3;i++)s+=Math.pow(a[i]-b[i],2);return Math.sqrt(s);}
    void findMatches(){if(matchRGB==null){toast("Choose a photo to compare first.");return;}ArrayList<Row> rows=db.all("");if(rows.isEmpty()){matches.setText("No saved colours yet. Save some first.");return;}
        Collections.sort(rows,(a,b)->Double.compare(dist(a.rgb,matchRGB),dist(b.rgb,matchRGB)));StringBuilder sb=new StringBuilder("Closest saved shades:\n\n");int limit=Math.min(5,rows.size());for(int i=0;i<limit;i++){Row r=rows.get(i);int score=Math.max(0,(int)Math.round(100*(1-dist(r.rgb,matchRGB)/442.0)));sb.append(i+1).append(". ").append(r.name).append(" — ").append(hex(r.rgb)).append("\nApprox. similarity: ").append(score).append("%\n");if(!r.quality.isEmpty())sb.append("Quality: ").append(r.quality).append("  ");if(!r.roll.isEmpty())sb.append("Roll: ").append(r.roll);sb.append("\n\n");}matches.setText(sb.toString());}
    void refreshLibrary(String q){if(libraryBox==null)return;libraryBox.removeAllViews();ArrayList<Row> rows=db.all(q);if(rows.isEmpty()){libraryBox.addView(text("No saved colours yet.",14));return;}
        for(Row r:rows){LinearLayout line=new LinearLayout(this);line.setOrientation(0);line.setPadding(0,dp(8),0,dp(8));ImageView im=new ImageView(this);try{im.setImageBitmap(BitmapFactory.decodeByteArray(r.photo,0,r.photo.length));}catch(Exception ignored){}im.setScaleType(ImageView.ScaleType.CENTER_CROP);line.addView(im,new LinearLayout.LayoutParams(dp(70),dp(70)));LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(dp(10),0,0,0);TextView t=text(r.name,16);t.setTypeface(null,1);col.addView(t);col.addView(text(hex(r.rgb)+"  "+r.quality,12));col.addView(text("Roll: "+r.roll,12));Button del=new Button(this);del.setText("Delete");del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Delete "+r.name+"?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{db.remove(r.id);refreshLibrary(search.getText().toString());}).show());col.addView(del);line.addView(col,new LinearLayout.LayoutParams(0,-2,1));libraryBox.addView(line);View divider=new View(this);divider.setBackgroundColor(Color.LTGRAY);libraryBox.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
    }
    void exportBackup(){try{File f=new File(getExternalFilesDir(null),"fabric-colour-backup.csv");ArrayList<Row> rows=db.all("");BufferedWriter w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));w.write("Name,Quality,Roll,Notes,HEX,RGB\n");for(Row r:rows)w.write(csv(r.name)+","+csv(r.quality)+","+csv(r.roll)+","+csv(r.notes)+","+hex(r.rgb)+","+csv(Arrays.toString(r.rgb))+"\n");w.close();Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/csv");share.putExtra(Intent.EXTRA_STREAM,android.net.Uri.fromFile(f));share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(share,"Save/share backup"));}catch(Exception e){toast("Backup could not be created.");}}
    String csv(String s){return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    static class Row{long id;String name,quality,roll,notes;byte[] photo;int[] rgb;Row(long i,String n,String q,String r,String no,byte[] p,int[] c){id=i;name=n;quality=q;roll=r;notes=no;photo=p;rgb=c;}}
    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"fabric_colours.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE colours (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,quality TEXT,roll TEXT,notes TEXT,photo BLOB,r INTEGER,g INTEGER,b INTEGER)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        void add(String name,String q,String roll,String notes,byte[] photo,int[] c){ContentValues v=new ContentValues();v.put("name",name);v.put("quality",q);v.put("roll",roll);v.put("notes",notes);v.put("photo",photo);v.put("r",c[0]);v.put("g",c[1]);v.put("b",c[2]);getWritableDatabase().insert("colours",null,v);}
        ArrayList<Row> all(String query){ArrayList<Row> a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM colours WHERE name LIKE ? OR quality LIKE ? OR roll LIKE ? ORDER BY name",new String[]{"%"+query+"%","%"+query+"%","%"+query+"%"});while(c.moveToNext())a.add(new Row(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getBlob(5),new int[]{c.getInt(6),c.getInt(7),c.getInt(8)}));c.close();return a;}
        void remove(long id){getWritableDatabase().delete("colours","id=?",new String[]{String.valueOf(id)});}
    }
}
