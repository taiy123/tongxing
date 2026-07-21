package com.tongxing.llama;

import android.content.res.AssetManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 同行 - Android原生LLama桥接模块
 * 
 * 编译要求：
 * 1. 在android/app/src/main/cpp/ 下放置 llama.cpp 源码
 * 2. 在CMakeLists.txt中添加JNI接口
 * 3. 将libllama.so链接到本项目
 * 
 * 简化方案：使用 react-native-llama 库替代此手动实现
 *    npm install react-native-llama
 */
public class LLamaBridgeModule extends ReactContextBaseJavaModule {
    private static final String TAG = "LLamaBridge";
    private static final String MODULE_NAME = "LLamaBridge";
    
    private final ReactApplicationContext reactContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long nativePtr = 0;
    private boolean modelLoaded = false;

    public LLamaBridgeModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    /**
     * 初始化模型
     * config: { modelPath, contextSize, threads, temperature, topP, topK, repeatPenalty }
     */
    @ReactMethod
    public void initModel(ReadableMap config, Promise promise) {
        executor.execute(() -> {
            try {
                String modelPath = config.getString("modelPath");
                int contextSize = config.hasKey("contextSize") ? config.getInt("contextSize") : 2048;
                int threads = config.hasKey("threads") ? config.getInt("threads") : 4;

                // 将assets中的模型复制到内部存储
                File modelFile = copyModelFromAssets(modelPath);
                if (modelFile == null || !modelFile.exists()) {
                    promise.reject("MODEL_NOT_FOUND", "模型文件未找到: " + modelPath);
                    return;
                }

                // TODO: 调用JNI加载模型
                // nativePtr = nativeInitModel(modelFile.getAbsolutePath(), contextSize, threads);
                
                modelLoaded = true;
                Log.i(TAG, "模型加载成功: " + modelFile.getAbsolutePath());
                
                WritableMap result = Arguments.createMap();
                result.putBoolean("success", true);
                result.putString("modelPath", modelFile.getAbsolutePath());
                promise.resolve(result);
            } catch (Exception e) {
                Log.e(TAG, "模型加载失败", e);
                promise.reject("INIT_ERROR", e.getMessage());
            }
        });
    }

    /**
     * 生成回复（非流式）
     */
    @ReactMethod
    public void completion(ReadableMap params, Promise promise) {
        executor.execute(() -> {
            try {
                if (!modelLoaded) {
                    promise.reject("MODEL_NOT_LOADED", "模型未加载");
                    return;
                }

                String prompt = params.getString("prompt");
                int nPredict = params.hasKey("nPredict") ? params.getInt("nPredict") : 512;
                float temperature = params.hasKey("temperature") ? 
                    (float) params.getDouble("temperature") : 0.7f;

                // TODO: 调用JNI推理
                // String response = nativeCompletion(nativePtr, prompt, nPredict, temperature);
                
                String response = "AI回复内容";
                promise.resolve(response);
            } catch (Exception e) {
                promise.reject("COMPLETION_ERROR", e.getMessage());
            }
        });
    }

    /**
     * 释放模型
     */
    @ReactMethod
    public void releaseModel(Promise promise) {
        executor.execute(() -> {
            try {
                if (nativePtr != 0) {
                    // nativeReleaseModel(nativePtr);
                    nativePtr = 0;
                }
                modelLoaded = false;
                promise.resolve(true);
            } catch (Exception e) {
                promise.reject("RELEASE_ERROR", e.getMessage());
            }
        });
    }

    /**
     * 检查模型是否已加载
     */
    @ReactMethod
    public void isModelLoaded(Promise promise) {
        promise.resolve(modelLoaded);
    }

    /**
     * 将模型从assets复制到内部存储
     */
    private File copyModelFromAssets(String modelPath) {
        try {
            // 目标路径: /data/data/com.tongxing/files/models/
            File modelsDir = new File(reactContext.getFilesDir(), "models");
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
            }

            String fileName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
            File targetFile = new File(modelsDir, fileName);

            // 如果已存在且大小一致，直接使用
            if (targetFile.exists()) {
                Log.i(TAG, "模型已缓存: " + targetFile.getAbsolutePath());
                return targetFile;
            }

            // 从assets复制
            AssetManager am = reactContext.getAssets();
            InputStream in = am.open(modelPath);
            OutputStream out = new FileOutputStream(targetFile);
            
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            in.close();
            out.close();
            
            Log.i(TAG, "模型复制完成: " + targetFile.getAbsolutePath() + 
                  " (" + targetFile.length() + " bytes)");
            return targetFile;
        } catch (Exception e) {
            Log.e(TAG, "模型复制失败", e);
            return null;
        }
    }

    // ---- Native JNI方法声明 ----
    // private native long nativeInitModel(String path, int ctxSize, int threads);
    // private native String nativeCompletion(long ptr, String prompt, int nPredict, float temp);
    // private native void nativeReleaseModel(long ptr);

    static {
        try {
            System.loadLibrary("llama_bridge");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "无法加载llama_bridge本地库: " + e.getMessage());
        }
    }
}
