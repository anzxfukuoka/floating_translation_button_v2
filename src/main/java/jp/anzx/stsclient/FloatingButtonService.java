package jp.anzx.stsclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class FloatingButtonService extends Service {

    public static final String TAG = "FloatingButtonService";

    public static final String CHANNEL_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private WindowManager windowManager;

    private ScreenshotTaker screenshotTaker;
    private TextRec textRec;
    private TSClient tsClient;

    private FloatingView floatingButton;
    private SelectionView selectionView;

    private FloatingView pointHelperStart;
    private FloatingView pointHelperEnd;
    private int halperSize = 42;

    private int LAYOUT_FLAG;

    private boolean isSelecting = false;


    class SelectionView extends View{

        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Paint mBitmapPaint;
        private Paint mPaint;
        private Context context;
        private Paint circlePaint;
        private Path circlePath;

        private float startX, startY;
        private float endX, endY;

        private int selectionColor = Color.RED;

        public WindowManager.LayoutParams params;

        private SelectionListener selectionListener;

        private boolean clearOnTouchUp = true;
        private boolean touchEnabled = true;

        public void setStartPos(float startX, float startY){
            this.startX = startX;
            this.startY = startY;
        }

        public void setEndPos(float endX, float endY){
            this.endX = endX;
            this.endY = endY;
        }

        public float getStartX() {
            return startX;
        }

        public float getStartY() {
            return startY;
        }

        public float getEndX() {
            return endX;
        }

        public float getEndY() {
            return endY;
        }

        public void setSelectionColor(int selectionColor) {
            this.selectionColor = selectionColor;
        }

        public void setClearOnTouchUp(boolean clearOnTouchUp){
            this.clearOnTouchUp = clearOnTouchUp;
        }

        public void setTouchEnabled(boolean touch) {
            this.touchEnabled = touch;
        }

        public SelectionView(Context context) {
            super(context);

            this.context = context;

            int color = (selectionColor & 0x00FFFFFF) | 0x88000000; //transparent 50%

            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            circlePaint = new Paint();
            circlePath = new Path();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.CYAN); //selectionColor
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeJoin(Paint.Join.MITER);
            circlePaint.setStrokeWidth(4f);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(color); // Color.parseColor("#66ff00ff")
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(12);
            mPaint.setTextSize(20);

            DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();

            WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            window.getDefaultDisplay().getRealMetrics(metrics);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath( circlePath,  circlePaint);
        }

        private void touch_start(float x, float y) {
            startX = x;
            startY = y;
        }

        private void touch_move(float x, float y) {
            endX = x;
            endY = y;

            drawSelection();
        }

        private void touch_up() {
            if(clearOnTouchUp){
                clearSelection();
                clearSelectionPos();
            }

        }

        public void drawSelection(){
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawRoundRect(startX, startY, endX, endY, 10, 10, mPaint);

            mCanvas.drawText("x: " + endX + ",y: " + endY, endX, endY, mPaint);

            invalidate(); //update
        }

        public void clearSelectionPos(){
            setStartPos(0, 0);
            setEndPos(0, 0);
        }

        public void clearSelection(){
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            invalidate(); //update
        }

        public boolean isTrivial(){
            return (startX == 0 && startY == 0 && endX == 0 && endY == 0);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if(!touchEnabled)
                return false;

            invalidate();

            //real coords
            PointF p = new PointF(event.getX(), event.getY());

            View v = this;// your view
            View root = v.getRootView();

            while (v.getParent() instanceof View && v.getParent() != root) {
                p.y += v.getTop();
                p.x += v.getLeft();
                v = (View) v.getParent();
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(p.x, p.y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(p.x, p.y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();

                    if(selectionListener != null){
                        selectionListener.onSelectionStop(startX, startY, endX, endY);
                    }

                    break;
            }
            return true;
        }

        public void setSelectionListener(SelectionListener selectionListener) {
            this.selectionListener = selectionListener;
        }

    }

    interface SelectionListener{
        void onSelectionStop(float startX, float startY, float endX, float endY);
    }

    class FloatingView extends androidx.appcompat.widget.AppCompatImageView{

        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;

        private int dpi = 1;

        public WindowManager.LayoutParams params;

        public FloatingViewListener floatingViewListener;

        public FloatingView(Context context) {
            super(context);

            dpi = (int)getApplication().getResources().getDisplayMetrics().density;

            params = new WindowManager.LayoutParams(
                    108 * dpi,//LayoutParams.WRAP_CONTENT,
                    108 * dpi,//LayoutParams.WRAP_CONTENT,
                    LAYOUT_FLAG, //LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x=0;
            params.y=100;

        }

        public void setFloatingViewListener(FloatingViewListener floatingViewListener) {
            this.floatingViewListener = floatingViewListener;
        }

        public void setSize(int sizeX, int sizeY){
            params.width = sizeX * dpi;
            params.height = sizeY * dpi;
        }

        public void setPos(int posX, int posY){
            params.x = posX;
            params.y = posY;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    //Listener
                    if(floatingViewListener != null)
                        floatingViewListener.onMoveStart(initialTouchX, initialTouchY);

                    break;
                case MotionEvent.ACTION_UP:
                    if((int) (event.getRawX() - initialTouchX) < 10 &&
                            (int) (event.getRawY() - initialTouchY) < 10){
                        Log.i(TAG, "click!");

                        callOnClick();

                        //add selection view
                        //mWindowManager.addView(selectionView, selectionParams);

                        //Listener
                        if(floatingViewListener != null)
                            floatingViewListener.onMoveEnd(params.x, params.y);

                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);

                    windowManager.updateViewLayout(this, params);

                    //Listener
                    if(floatingViewListener != null)
                        floatingViewListener.onMove(params.x, params.y);

                    break;
            }
            return false;
        }
    }

    interface FloatingViewListener{
         public void onMoveStart(float posX, float posY);
         public void onMoveEnd(float posX, float posY);
         public void onMove(float posX, float posY);
    }

    public FloatingButtonService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //sdk 29
        startForeground(2268, getForegroundNotification(getString(R.string.app_name), "♥"));

        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);


        Display mDisplay = windowManager.getDefaultDisplay();
        Point size = new Point();
        mDisplay.getRealSize(size);


        screenshotTaker = new ScreenshotTaker(getApplicationContext(), Perms.mediaProjectionResultCode, Perms.mediaProjectionData, size.x, size.y);
        screenshotTaker.setNavigationBarHeight(MainActivity.navigationBarHeight);
        screenshotTaker.setStatusBarHeight(MainActivity.statusBarHeight);

        textRec = new TextRec(getApplicationContext());
        tsClient = new TSClient();



        floatingButton = new FloatingView(this);
        floatingButton.setImageResource(R.drawable.ic_wingicon);
        floatingButton.setSize( 80, 80);

        selectionView = new SelectionView(this);
        selectionView.setClearOnTouchUp(false);

        pointHelperStart = new FloatingView(this);
        pointHelperStart.setImageResource(R.drawable.ic_twotone_circle_24);
        pointHelperStart.setSize(halperSize, halperSize);

        pointHelperEnd = new FloatingView(this);
        pointHelperEnd.setImageResource(R.drawable.ic_twotone_circle_24);
        pointHelperEnd.setSize(halperSize, halperSize);

        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isSelecting){

                    //Toast.makeText(FloatingButtonService.this, selectionView.getStartX() + " " + selectionView.getEndX(), Toast.LENGTH_SHORT).show();

                    if(selectionView.isTrivial()){

                        selectionView.setTouchEnabled(true);

                        //remove
                        if(pointHelperStart.getWindowToken() != null)
                            windowManager.removeView(pointHelperStart);
                        if(pointHelperEnd.getWindowToken() != null)
                            windowManager.removeView(pointHelperEnd);
                        if(selectionView.getWindowToken() != null)
                            windowManager.removeView(selectionView);

                        //
                    }else{
                        
                        selectionView.clearSelection();

                        //remove
                        if(pointHelperStart.getWindowToken() != null)
                            windowManager.removeView(pointHelperStart);
                        if(pointHelperEnd.getWindowToken() != null)
                            windowManager.removeView(pointHelperEnd);
                        if(selectionView.getWindowToken() != null)
                            windowManager.removeView(selectionView);

                        screenshotTaker.takeScreenshot();
                    }

                    floatingButton.setImageResource(R.drawable.ic_wingicon);

                    isSelecting = false;
                }else{
                    //start selecting
                    windowManager.addView(selectionView, selectionView.params);

                    floatingButton.setImageResource(R.drawable.ic_wingicon_red);

                    //on top
                    windowManager.removeView(floatingButton);
                    windowManager.addView(floatingButton, floatingButton.params);

                    isSelecting = true;
                }
                //Toast.makeText(FloatingButtonService.this, "click!", Toast.LENGTH_SHORT).show();
            }
        });

        selectionView.setSelectionListener(new SelectionListener() {
            @Override
            public void onSelectionStop(float startX, float startY, float endX, float endY) {

                selectionView.setTouchEnabled(false);

                pointHelperStart.setPos((int)startX - halperSize, (int)startY - halperSize);
                pointHelperEnd.setPos((int)endX - halperSize, (int)endY - halperSize);

                windowManager.addView(pointHelperStart, pointHelperStart.params);
                windowManager.addView(pointHelperEnd, pointHelperEnd.params);
            }
        });

        pointHelperStart.setFloatingViewListener(new FloatingViewListener() {
            @Override
            public void onMoveStart(float posX, float posY) {

            }

            @Override
            public void onMoveEnd(float posX, float posY) {

            }

            @Override
            public void onMove(float posX, float posY) {

                selectionView.setStartPos(posX + halperSize, posY + halperSize);
                selectionView.drawSelection(); //update
            }
        });

        pointHelperEnd.setFloatingViewListener(new FloatingViewListener() {
            @Override
            public void onMoveStart(float posX, float posY) {

            }

            @Override
            public void onMoveEnd(float posX, float posY) {

            }

            @Override
            public void onMove(float posX, float posY) {
                selectionView.setEndPos(posX + halperSize, posY + halperSize);
                selectionView.drawSelection(); //update

                //Log.i(TAG, "○");

            }
        });

        screenshotTaker.setScreenshotTakerListener(new ScreenshotTaker.ScreenshotTakerListener() {
            @Override
            public void onImageReady(Bitmap image) {

                //image = Bitmap.createScaledBitmap(image, selectionView.getWidth(), selectionView.getHeight(), false);
                Bitmap result = ScreenshotTaker.cropBitmap(image, (int)selectionView.startX, (int)selectionView.startY, (int)selectionView.endX, (int)selectionView.endY);

                selectionView.clearSelectionPos();
                selectionView.setTouchEnabled(true);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int langFromId = preferences.getInt("lang_from", 0);
                int langToId = preferences.getInt("lang_to", 1);

                String rec = textRec.extractText(result, Lang.LL3[langFromId]);
                Log.i(TAG, rec);

                tsClient.setLangFrom(Lang.LL2[langFromId]);
                tsClient.setLangTo(Lang.LL2[langToId]);
                tsClient.TS(rec);

                screenshotTaker.saveBitmap(result);
                //screenshotTaker.saveBitmap(image);
            }
        });
        
        tsClient.setTsClientListener(new TSClient.TSClientListener() {
            @Override
            public void OnTranslated(String text) {
                Toast.makeText(FloatingButtonService.this, text, Toast.LENGTH_LONG).show();
            }
        });

        windowManager.addView(floatingButton, floatingButton.params);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //startForeground(2268, getForegroundNotification(getString(R.string.app_name), "○"));

    }

    Notification getForegroundNotification(String title, String text){

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "channel",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("channel description");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(false);

            notificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true);

            notification = builder.build();

        } else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            notification = builder.build();
        }

        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(floatingButton != null && floatingButton.getWindowToken() != null){
            windowManager.removeView(floatingButton);
        }
        if(selectionView != null && selectionView.getWindowToken() != null){
            windowManager.removeView(selectionView);
        }
        if(pointHelperStart != null && pointHelperStart.getWindowToken() != null){
            windowManager.removeView(pointHelperStart);
        }
        if(pointHelperEnd != null && pointHelperEnd.getWindowToken() != null){
            windowManager.removeView(pointHelperEnd);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
