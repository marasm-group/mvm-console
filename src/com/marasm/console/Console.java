package com.marasm.console;

import com.marasm.ppc.CTRL;
import com.marasm.ppc.PPC;
import com.marasm.ppc.PPCDevice;
import com.marasm.ppc.Variable;

/**
 * Created by sr3u on 20.08.15.
 */
public class Console extends PPCDevice
{
    final String ctrlPort="63";
    final String dataPort="64";
    final String charPort="65";

    String ctrlBuf=new String();
    String charBuf=new String();

    public String manufacturer() {return "marasm";}
    public void connected() {
        PPC.connect(new Variable(ctrlPort), this);
        PPC.connect(new Variable(dataPort), this);
        PPC.connect(new Variable(charPort), this);
        C.io.setTitle("MVM Console");
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
}
