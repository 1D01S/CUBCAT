//package com.a80;
//
//import android.content.Context;
//import android.util.Log;
//
//import java.io.InputStream;
//
//public class OBJRenderer {
//    private static final String TAG = "OBJRenderer";
//    private ObjModel objModel;
//    private boolean isModelLoaded = false;
//
//    // Конструктор, принимающий поток данных модели и шейдерную программу
//    public OBJRenderer(Context context, InputStream objInputStream, int shaderProgram) {
//        loadModel(objInputStream, shaderProgram);
//    }
//
//    private void loadModel(InputStream objInputStream, int shaderProgram) {
//        if (objInputStream == null) {
//            Log.e(TAG, "Model input stream is null.");
//            return;
//        }
//        // Создаем модель с кастомной шейдерной программой
//        objModel = new ObjModel(objInputStream, shaderProgram);
//        isModelLoaded = true;
//        Log.d(TAG, "Model loaded successfully.");
//    }
//
//    public void render(float[] mvpMatrix) {
//        if (!isModelLoaded || objModel == null) {
//            Log.e(TAG, "Model is not loaded or initialized.");
//            return;
//        }
//        objModel.draw(mvpMatrix);
//    }
//
//    public void destroy() {
//        if (objModel != null) {
//            objModel.destroy();
//            objModel = null;
//            isModelLoaded = false;
//            Log.d(TAG, "Model resources released.");
//        }
//    }
//}

