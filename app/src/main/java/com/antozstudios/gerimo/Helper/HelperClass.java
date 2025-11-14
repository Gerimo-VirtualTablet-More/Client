package com.antozstudios.gerimo.Helper;

import android.util.Log;

import com.antozstudios.gerimo.Helper.Records.ScreenData;

import java.util.ArrayList;

public class  HelperClass{





    public static ArrayList<ScreenData> getAllScreenData(String commandData) {
        ArrayList<ScreenData> screenDataArrayList = new ArrayList<>();

        String[] splitScreenData = commandData.split(",");

        for(int i=0;i<splitScreenData.length;i++){
            String[] getSplitData = splitScreenData[i].split(";");
            boolean isPrimary = Boolean.parseBoolean(getSplitData[0]);
            String deviceName = getSplitData[1];
            int workareaWidth = Integer.parseInt(getSplitData[2]);
            int workareaHeight = Integer.parseInt(getSplitData[3]);
            int workareaX = Integer.parseInt(getSplitData[4]);
            int workareaY = Integer.parseInt(getSplitData[5]);
            int workareaBottom = Integer.parseInt(getSplitData[6]);
            int workareaTop = Integer.parseInt(getSplitData[7]);
            int workareaLeft= Integer.parseInt(getSplitData[8]);
            int workareaRight = Integer.parseInt(getSplitData[9]);

            Log.d("mmm",workareaWidth + " " + workareaHeight + " " + workareaX + " " + workareaY);

            screenDataArrayList.add(new ScreenData(isPrimary,deviceName,workareaWidth,workareaHeight,workareaX,workareaY,workareaBottom,workareaTop,workareaLeft,workareaRight));
        }


        return screenDataArrayList;
    }



}