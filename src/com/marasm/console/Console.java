package com.marasm.console;

import com.marasm.ppc.CTRL;
import com.marasm.ppc.PPC;
import com.marasm.ppc.PPCDevice;
import com.marasm.ppc.Variable;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by sr3u on 20.08.15.
 */
public class Console extends PPCDevice
{
    final String ctrlPort="63.0";
    final String dataPort="63.1";
    final String charPort="63.2";

    String ctrlBuf=new String();
    String charBuf=new String();
    static final String defaultSettings="{\n"
            +"\"background\":{\"color\":{\"red\":0,\"green\":0,\"blue\":0}},"
            +"\n\"foreground\":{\"color\":{\"red\":0,\"green\":255,\"blue\":0}},"
            +"\n\"caret\":{\"color\":{\"red\":0,\"green\":255,\"blue\":0}},"
            +"\n\"user\":{\"color\":{\"red\":255,\"green\":255,\"blue\":255}},"
            +"\n\"font\":{\"name\":\"Lucida Console\",\"size\":14},\n"
            +"\"redirectPrintStreams\":true\n}";
    public String jarLocation()
    {
        return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }
    public String manufacturer() {return "marasm";}
    public void connected() {
        C.io.setTitle("MVM Console");
        String jsonLoc=jarLocation() + "console.json";
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
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            JSONObject color = config.getJSONObject("background").getJSONObject("color");
            C.io.setBackgroundColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));
            color = config.getJSONObject("caret").getJSONObject("color");
            C.io.setCaretColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));
            color = config.getJSONObject("user").getJSONObject("color");
            C.io.setPromptColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));
            color = config.getJSONObject("foreground").getJSONObject("color");
            C.io.setTextColor(new Color(color.getInt("red"), color.getInt("green"), color.getInt("blue")));
            if(config.getBoolean("redirectStdIO")){C.io.redirectSystemStreams();}
            try{C.io.setTitle(config.getString("windowTitle"));}catch (JSONException e){}
        }catch (JSONException e){
            try {
                PrintWriter fw = new PrintWriter(jsonLoc, "UTF8");
                fw.println(defaultSettings);
                fw.close();
                fw.flush();
                config=new JSONObject(defaultSettings);
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
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
                if(d.equals(CTRL.NOP.toString())){return;}
                if(d.equals(CTRL.GETMAN.toString())){
                    ctrlBuf=manufacturer();
                    return;}
                break;
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
                if(ctrlBuf.length()>0)
                {
                    if(ctrlBuf.length()==0) {ctrlBuf=new String();}
                    if(ctrlBuf.length()<2){
                        Variable tmp=new Variable(ctrlBuf.substring(0,1).getBytes()[0]);
                        ctrlBuf=ctrlBuf.substring(1);
                        return tmp;
                    }
                    else{
                        Variable v=new Variable(ctrlBuf.substring(0,1).getBytes()[0]);
                        ctrlBuf=ctrlBuf.substring(1);
                        return v;
                    }
                }
                return new Variable();
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
        PPC.out(new Variable(con.dataPort), new Variable(100));
        PPC.in(new Variable(con.dataPort));
        System.out.println("HELLO!");
    }
}
