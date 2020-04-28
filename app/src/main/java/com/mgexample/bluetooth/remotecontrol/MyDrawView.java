package com.mgexample.bluetooth.remotecontrol;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;

public class MyDrawView extends View {

    private int Idx = 0, mWindowSizeFlag = 1, mMaxAxisXDim = 1000;

    private int RemoterType = 0; //0 means four button, 1 means anther type
    private int SimpleTypeButtonNum = 4;
    private int[] ButtonShowFlagArray = new int[50];
    private long[] ButtonTOData = new long[50];

    public void Init(int WindowSizeFlag, int max_X) {
        Idx = 0;
        mWindowSizeFlag = WindowSizeFlag;

        mMaxAxisXDim = 1000;
        if (mWindowSizeFlag != 1) mMaxAxisXDim = max_X - 20;

        RemoterType = 1;//four button type, default type

        int i;
        for (i = 0; i < 50; i++) {
            ButtonShowFlagArray[i] = 0;
            ButtonTOData[i] = 0;
        }
    }

    ;

    public void SettingRemoteType(int t) {
        RemoterType = t;
    }

    public void CmdComing(int id, long curTime) {
        int i;
        for (i = 0; i < 49; i++) {
            ButtonTOData[i] = 0;//clear all old data, only one buttone can be enabled
        }
        if (id <= 48) ButtonTOData[id] = curTime + 500;
        UpdateItems(curTime);
    }

    public void UpdateAll() {
        postInvalidate();
    }

    public void UpdateItems(long curTime) {
        //scan all button, wether it is timeout or not
        int i;

        for (i = 0; i < 50; i++) {
            if (ButtonTOData[i] < curTime)//time out
            {
                ButtonShowFlagArray[i] = 0;
            } else {
                ButtonShowFlagArray[i] = 1;
            }
        }

        postInvalidate();
    }

    private int found_mg_ctl_flag = 0;

    public int AddOneRec(int rssi, int v, byte[] data, long CurAdvTime, long aa) {
        int i;
        byte L, flag, Op1, Op2, p1, p2, p3;
        byte gid, aa0, aa1, aa2, aa3;
        int ButtonId = 20;
        int pos_offset = 0;
        long local_aa = -1;

        found_mg_ctl_flag = 0;

        i = 0;
        while (i < data.length/*32*/) {
            L = data[i];
            if (L == 0) break;

            flag = data[i + 1];
            if ((flag == 0x07) || (flag == (byte) 0xff)) {
                if (flag == 0x07) pos_offset = 4;
                else pos_offset = 0;

                if ((data[i + 2] == 0x47) && (data[i + 3] == 0x4d))//found
                {
                    gid = data[i + 4 + pos_offset];
                    aa0 = data[i + 5 + pos_offset];
                    aa1 = data[i + 6 + pos_offset];
                    aa2 = data[i + 7 + pos_offset];
                    aa3 = data[i + 8 + pos_offset];

                    local_aa = ((aa0 << 24) & 0x00FF000000) + ((aa1 << 16) & 0x00FF0000) + ((aa2 << 8) & 0x00FF00) + ((aa3) & 0x00FF);
                    if (local_aa != aa) return 0;

                    found_mg_ctl_flag = 1;
                    ButtonTOData[49] = CurAdvTime + 200; //ID 49 is the cmd indication flash light

                    Op1 = data[i + 9 + pos_offset];
                    Op2 = data[i + 10 + pos_offset];
                    p1 = data[i + 11 + pos_offset];
                    p2 = data[i + 12 + pos_offset];
                    p3 = data[i + 13 + pos_offset];

                    if ((Op1 == (-1)) && (Op2 == (-1))) //0xff,0xff, simple type remote
                    {
                        RemoterType = 0;//simple type
                        ButtonId = p1;

                        CmdComing(ButtonId, CurAdvTime);
                        break;
                    } else//complex type
                    {
                        RemoterType = 1;
                    }

                    if ((byte) (gid & 0x80) != 0) //0x80
                    {
                        gid = (byte) 0xff;//all group cmd
                    }

                    if ((Op1 == 0x01) && (Op2 == 0x02)) //on off
                    {
                        //if(p1 == 0x01)led_on_off = 1;
                        //else led_on_off = 0;
                        if (gid == (-1)) //0xff
                        {
                            if (p1 == 0x01) ButtonId = 0;
                            else ButtonId = 1;
                        } else {
                            if (p1 == 0x01) ButtonId = 6 + gid * 2;
                            else ButtonId = 7 + gid * 2;
                        }
                    } else if ((Op1 == 0x02) && (Op2 == 0x01)) //RGB
                    {
                        // mg_r = p1; mg_r&=0x00ff;
                        // mg_g = p2; mg_g&=0x00ff;
                        // mg_b = p3; mg_b&=0x00ff;
                    } else if ((Op1 == 0x02) && (Op2 == 0x02)) //yw
                    {
                        if (p1 == 0x01)//Y
                        {
                            //   mg_r = 255;
                            //   mg_g = 165;
                            //   mg_b = 0;
                        } else {//W
                            //   mg_r = 250;
                            //   mg_g = 250;
                            //   mg_b = 250;
                        }
                    } else if ((Op1 == 0x02) && (Op2 == 0x03))//brightness
                    {
                        // led_lum = p1;
                    } else if ((Op1 == 0x02) && (Op2 == 0x04))//lum +/-
                    {
                        if (p1 == 0x01)//add
                        {
                            ButtonId = 4;
                            // led_lum += 5;
                            // if(led_lum > 100)led_lum = 100;
                        } else//minus
                        {
                            ButtonId = 5;
                            // led_lum -= 5;
                            // if(led_lum < 5)led_lum = 5;
                        }
                    } else if ((Op1 == 0x03) && (Op2 == 0x02))//mode +/-)
                    {
                        if (p1 == 0x01)//add
                        {
                            ButtonId = 3;
                        } else {
                            ButtonId = 2;
                        }
                    }

                    CmdComing(ButtonId, CurAdvTime);

                    break;
                } else// mg former format
                {
                    return 0; //if want to support, then addr must be filted.
/*
                    SimpleTypeButtonNum = 4; //default
                    //if(L==3)
                    {
                        RemoterType = 0;//simple type
                        ButtonId = data[i+2]&0xFF;

                        if(ButtonId > 0x0F)
                        {
                            //two button type
                            SimpleTypeButtonNum = 2;
                            switch (ButtonId)//0x20, 0x30
                            {
                                case 0x20:
                                    ButtonId = 0;
                                    break;
                                case 0x30:
                                    ButtonId = 1;
                                    break;
                                default:
                                    ButtonId = 0;
                                    break;
                            }
                        }
                        else {
                            //four button type
                            SimpleTypeButtonNum = 4;
                            if (ButtonId > 3) ButtonId = 3;

                            switch (ButtonId) {
                                case 2:
                                    ButtonId = 0;
                                    break;
                                case 0:
                                    ButtonId = 2;
                                    break;
                                default:
                                    break;
                            }
                        }
                        CmdComing(ButtonId, CurAdvTime);
                        break;
                    }
*/
                }
            }
            i = i + (L + 1);
        }

        postInvalidate();

        return 1;
    }

    public MyDrawView(Context context) {
        super(context);
    }

    private void mgDrawCircleButton(Canvas canvas, Paint paint, int x, int y, int r, String cs, int OnOffFlag) {
        if (OnOffFlag == 1) {
            paint.setColor(Color.BLACK);
            canvas.drawCircle(x, y, r, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, r - 2, paint);
            paint.setColor(Color.MAGENTA);
            canvas.drawCircle(x, y, r - 4, paint);
        } else {
            paint.setColor(Color.BLACK);
            canvas.drawCircle(x, y, r, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, r - 2, paint);
            paint.setColor(Color.GRAY);
            canvas.drawCircle(x, y, r - 4, paint);
        }

        paint.setColor(Color.BLACK);
        paint.setTextSize(32);
        canvas.drawText(cs, x - (1) * 12, y + 10, paint);
    }

    private void mgDrawRecButton(Canvas canvas, Paint paint, int x, int y, int w, int h, String cs, int OnOffFlag) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + w, y + h, paint);
        paint.setColor(Color.GRAY);
        canvas.drawRect(x + 2, y + 2, x + w - 2, y + h - 2, paint);

        if (OnOffFlag == 1) {
            paint.setColor(Color.MAGENTA);
            canvas.drawRect(x + 6, y + 6, x + w - 2, y + h - 2, paint);

            paint.setColor(Color.BLACK);
            canvas.drawLine(x + 2, y + 2, x + 6, y + 6, paint);
            paint.setColor(Color.WHITE);
            canvas.drawLine(x + 3, y + h - 3, x + w - 3, y + h - 3, paint);
            canvas.drawLine(x + w - 3, y + 4, x + w - 3, y + h - 3, paint);
        } else {
            paint.setColor(Color.WHITE);
            canvas.drawRect(x + 2, y + 2, x + w - 2, y + h - 2, paint);
            paint.setColor(Color.GRAY);
            canvas.drawRect(x + 2, y + 2, x + w - 4, y + h - 4, paint);
        }
        paint.setColor(Color.BLACK);
        paint.setTextSize(32);
        canvas.drawText(cs, x + w / 2 - 10, y + h / 2 + 16, paint);
    }

    private void mgDrawFrame(Canvas canvas, Paint paint, int x, int y, int w, int h, int color) {
        paint.setColor(color);
        canvas.drawLine(x, y, x + w, y, paint);
        canvas.drawLine(x + w, y, x + w, y + h, paint);
        canvas.drawLine(x, y + h, x + w, y + h, paint);
        canvas.drawLine(x, y + h, x, y, paint);
    }

    private void ShowBmpPic(Canvas canvas, Paint paint, Bitmap bm, int x, int y, int Dim) {
        int i, j, w, h;
        int p, bk_color = bm.getPixel(0, 0);
        Bitmap nbm;
        Rect src = new Rect();
        RectF ds = new RectF();

        w = bm.getWidth();
        h = bm.getHeight();
        nbm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (i = 0; i < w; i++) {
            for (j = 0; j < h; j++) {
                p = bm.getPixel(i, j);
                if (p != bk_color) {
                    nbm.setPixel(i, j, p);
                }
            }
        }

        src.set(0, 0, w, h);
        ;
        ds.set(x, y, x + w / Dim, y + h / Dim);
        //canvas.drawBitmap(nbm,x,y,paint);
        canvas.drawBitmap(nbm, src, ds, paint);
    }

    private int OffsetX = 50, OffsetY = 50;

    @Override
    public void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        int type0_x[] = {50, 250, 50, 250, 225};
        int type0_y[] = {50, 50, 250, 250, 225};
        String string_type0[] = {"A", "B", "C", "D"};
        int dim = 2;

        Bitmap bm;
        Paint paint = new Paint();

        bm = BitmapFactory.decodeResource(getResources(), R.drawable.mgicon);
        OffsetX = (mMaxAxisXDim - 450) / 2;
        OffsetY = OffsetX;

        if (mMaxAxisXDim < 1000) dim = 1;

        if (RemoterType == 0)//four buttone type
        {
            mgDrawFrame(canvas, paint, OffsetX, OffsetY, 450, 650, Color.BLACK);
            int i;
            for (i = 0; i < SimpleTypeButtonNum; i++) {
                mgDrawRecButton(canvas, paint, type0_x[i] + OffsetX, type0_y[i] + OffsetY, 150, 150, string_type0[i], ButtonShowFlagArray[i]);
            }
            mgDrawCircleButton(canvas, paint, OffsetX + type0_x[4], OffsetY + type0_y[4], 15, " ", ButtonShowFlagArray[49]);//indication flash light

            paint.setTextSize(64);
            paint.setColor(Color.BLACK);
            canvas.drawText("MacroGiga", 60 + OffsetX, 500 + OffsetY, paint);
            canvas.drawText("Demo", 150 + OffsetX, 600 + OffsetY, paint);
            //ShowBmpPic(canvas,paint,bm,100+OffsetX,460+OffsetY,dim);
        } else//complex type
        {
            mgDrawCircleButton(canvas, paint, mMaxAxisXDim / 2, 80, 50, " I", ButtonShowFlagArray[0]);
            mgDrawCircleButton(canvas, paint, mMaxAxisXDim / 2, 200, 50, "O", ButtonShowFlagArray[1]);

            mgDrawRecButton(canvas, paint, mMaxAxisXDim / 4 - 50, 345, 100, 150, "<|", ButtonShowFlagArray[2]);
            mgDrawRecButton(canvas, paint, mMaxAxisXDim * 3 / 4 - 50, 345, 100, 150, "|>", ButtonShowFlagArray[3]);
            mgDrawRecButton(canvas, paint, mMaxAxisXDim / 2 - 75, 270, 150, 100, "^", ButtonShowFlagArray[4]);
            mgDrawRecButton(canvas, paint, mMaxAxisXDim / 2 - 75, 470, 150, 100, "v", ButtonShowFlagArray[5]);

            int i;
            for (i = 0; i < 4; i++) {
                mgDrawCircleButton(canvas, paint, mMaxAxisXDim / 4 + i * (mMaxAxisXDim / 6), 630, 25, " I", ButtonShowFlagArray[6 + i * 2]);
                mgDrawCircleButton(canvas, paint, mMaxAxisXDim / 4 + i * (mMaxAxisXDim / 6), 685, 25, "O", ButtonShowFlagArray[7 + i * 2]);
            }

            mgDrawCircleButton(canvas, paint, mMaxAxisXDim * 3 / 4, 80, 15, " ", ButtonShowFlagArray[49]);//indication flash light

            paint.setTextSize(64);
            paint.setColor(Color.BLACK);
            canvas.drawText("MacroGiga", 60 + OffsetX, 780, paint);
            canvas.drawText("Demo", 150 + OffsetX, 860, paint);
            //ShowBmpPic(canvas,paint,bm,100+OffsetX,730,dim);

            mgDrawFrame(canvas, paint, mMaxAxisXDim / 4 - 100, 15, mMaxAxisXDim / 2 + 200, 860, Color.BLACK);
        }

        super.onDraw(canvas);
    }

}