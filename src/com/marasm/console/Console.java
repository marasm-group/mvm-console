package com.marasm.console;

import com.marasm.ppc.CTRL;
import com.marasm.ppc.PPC;
import com.marasm.ppc.PPCDevice;
import com.marasm.ppc.Variable;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;

/**
 * Created by sr3u on 20.08.15.
 */
public class Console extends PPCDevice
{
    final String ctrlPort="63.0";
    final String dataPort="63.1";
    final String charPort="63.2";

    String charBuf=new String();
    static final String defaultSettings="{\n"
            +"\"background\":{\"color\":{\"red\":0,\"green\":0,\"blue\":0}},"
            +"\n\"foreground\":{\"color\":{\"red\":0,\"green\":255,\"blue\":0}},"
            +"\n\"caret\":{\"color\":{\"red\":0,\"green\":255,\"blue\":0}},"
            +"\n\"user\":{\"color\":{\"red\":255,\"green\":255,\"blue\":255}},"
            +"\n\"font\":{\"name\":\"Lucida Console\",\"size\":14,\"style:\":15},\n"
            +"\"redirectStdIO\":true\n}";
    public String jarLocation()
    {
        String path=this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if(path.endsWith("!/")){path=path.substring(0,path.length()-2);}
        String fileName=path.substring(path.lastIndexOf(File.separatorChar) + 1);
        if(fileName.contains(".jar")){path=path.substring(0, path.lastIndexOf(File.separatorChar)+1).trim();}
        if(path.startsWith("file:")){path=path.substring(5);}
        return path.trim();
    }
    @Override
    public String manufacturer() {return "marasm";}
    public void connected() {
        C.io.setTitle("MVM Console");
        String jsonLoc=jarLocation() + "console.json";
        jsonLoc=jsonLoc.trim();
        jsonLoc=jsonLoc.replaceAll("[%]20"," ");
        System.out.println(jsonLoc);
        FileReader jsonReader;
        JSONObject config=new JSONObject(defaultSettings);
        try {
            jsonReader = new FileReader(jsonLoc);
            config=new JSONObject(readAsString(jsonReader));
        } catch (IOException | JSONException e) {
            try {
                PrintWriter fw = new PrintWriter(jsonLoc, "UTF8");
                fw.println(defaultSettings);
                fw.close();
                fw.flush();
                config=new JSONObject(defaultSettings);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            try{JSONObject font=config.getJSONObject("font");
                if(font.has("style")){C.io.setFont(font.getString("name"),font.getInt("style"),font.getInt("size"));}
                else{C.io.setFont(font.getString("name"),0,font.getInt("size"));}
            }catch (JSONException e){}
            JSONObject color;
            try{color = config.getJSONObject("background").getJSONObject("color");
            C.io.setBackgroundColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));}catch (JSONException e){}
            try{color = config.getJSONObject("caret").getJSONObject("color");
            C.io.setCaretColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));}catch (JSONException e){}
            try{color = config.getJSONObject("user").getJSONObject("color");
            C.io.setPromptColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));}catch (JSONException e){}
            try{color = config.getJSONObject("foreground").getJSONObject("color");
            C.io.setTextColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));}catch (JSONException e){}
            if(config.getBoolean("redirectStdIO")){C.io.redirectSystemStreams();}
            try{C.io.setTitle(config.getString("windowTitle"));}catch (JSONException e){}
        }catch (JSONException e){
            try {
                PrintWriter fw = new PrintWriter(jsonLoc, "UTF8");
                fw.println(defaultSettings);
                fw.close();
                fw.flush();
                config=new JSONObject(defaultSettings);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {Thread.sleep(100);}catch (InterruptedException e) {}
        PPC.connect(new Variable(ctrlPort), this);
        PPC.connect(new Variable(dataPort), this);
        PPC.connect(new Variable(charPort), this);
    }
    public void out(Variable port, Variable data)
    {
        String p=port.toString();
        String d=data.toString();
        switch (p)
        {
            case ctrlPort:
                ctrlOut(data);
            case dataPort:
                C.io.print(d);
                break;
            case charPort:
                char chr=(char)data.intValue();
                if(chr==0){return;}
                C.io.print(chr);
                break;
            default:
                return;
        }
    }
    public Variable in(Variable port)
    {
        String p=port.toString();
        switch (p)
        {
            case ctrlPort:
                return ctrlIn();
            case dataPort:
                return new Variable(C.io.nextLine());
            case charPort:
                while(charBuf.length()==0){charBuf=C.io.nextLine();}
                Variable tmp=new Variable(charBuf.substring(0,1).getBytes()[0]);
                if(charBuf.length()<2) {charBuf=new String();}
                else{charBuf=charBuf.substring(1);}
                return tmp;
            default:
                break;
        }
        return new Variable();
    }
    private String readAsString(Reader r) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(r);
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
    public static void main(String[]args)
    {
        Console con=new Console();
        con.connected();
        Variable v=new Variable(-1);
        PPC.out(new Variable(con.ctrlPort), new Variable(CTRL.GETMAN));
        while(!v.equals(new Variable(0)))
        {
            v=PPC.in(new Variable(con.ctrlPort));
            PPC.out(new Variable(con.charPort),v);
        } PPC.out(new Variable(con.charPort),new Variable(0));
        PPC.out(new Variable(con.dataPort), new Variable(100));
        PPC.in(new Variable(con.dataPort));
        System.out.println("HELLO!");
    }
}
