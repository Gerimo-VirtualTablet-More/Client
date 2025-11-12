package com.antozstudios.drawnow.Helper.Records;

//Screen[Bounds={X=0,Y=0,Width=1920,Height=1080} WorkingArea={X=0,Y=0,Width=1920,Height=1080} Primary=True DeviceName=DISPLAY1
//Screen[Bounds={X=1920,Y=0,Width=2712,Height=1220} WorkingArea={X=1920,Y=0,Width=2712,Height=1220} Primary=False DeviceName=DISPLAY22

public record ScreenData(boolean isPrimary, String deviceName, int workareaWidth, int workareaHeight, int workareaX, int workareaY, int workareaBottom, int workareaTop, int workareaLeft, int workareaRight) { }
