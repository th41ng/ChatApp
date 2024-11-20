package com.example.chatapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static boolean isInitialized = false;

    public static void initialize(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "ddskv3qix");
            config.put("api_key", "237429289958929");
            config.put("api_secret", "72Fe5rWNVv0_3E8fAHa9lvZ2zGk");
            MediaManager.init(context, config);
            isInitialized = true;  // Đánh dấu đã được khởi tạo
        }
    }
}
