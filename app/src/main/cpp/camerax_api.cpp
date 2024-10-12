#include <jni.h>
#include <string>
#include <unistd.h>
#include <android/bitmap.h>
#include <cmath>

extern "C" {
    JNIEXPORT jobject JNICALL
    Java_com_example_testapp_camerax_CameraViewModel_nativeRotateAndScale(
            JNIEnv *env,
            jobject instance,
            jobject bitmap,
            jfloat rotationDegrees
    ) {
        AndroidBitmapInfo info;
        void *pixels;

        if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
            AndroidBitmap_unlockPixels(env,bitmap);
            return nullptr;
        }

        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
            AndroidBitmap_unlockPixels(env,bitmap);
            return nullptr;
        }

        uint32_t newWidth, newHeight;
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            newWidth = info.height;
            newHeight = info.width;
        } else {
            newWidth = info.width;
            newHeight = info.height;
        }

        jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
        jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                              "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        jstring configName = env->NewStringUTF("ARGB_8888");
        jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
        jmethodID valueOfConfigMethod = env->GetStaticMethodID(configClass, "valueOf",
                                                               "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
        jobject bitmapConfig = env->CallStaticObjectMethod(configClass, valueOfConfigMethod,
                                                           configName);

        jobject newBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, (int)newWidth,
                                                        (int)newHeight, bitmapConfig);

        void *newPixels;
        if (AndroidBitmap_lockPixels(env, newBitmap, &newPixels) < 0) {
            AndroidBitmap_unlockPixels(env, bitmap);
            AndroidBitmap_unlockPixels(env, newBitmap);
            return nullptr;
        }

        auto *src = (uint32_t *) pixels;
        auto *dst = (uint32_t *) newPixels;

        for (int y = 0; y < info.height; ++y) {
            for (int x = 0; x < info.width; ++x) {
                uint32_t newX, newY;
                switch ((int) rotationDegrees) {
                    case 90:
                        newX = info.height - 1 - y;
                        newY = x;
                        break;
                    case 180:
                        newX = info.width - 1 - x;
                        newY = info.height - 1 - y;
                        break;
                    case 270:
                        newX = y;
                        newY = info.width - 1 - x;
                        break;
                    default:
                        newX = x;
                        newY = y;
                        break;
                }
                dst[newY * newWidth + newX] = src[y * info.width + x];
            }
        }

        AndroidBitmap_unlockPixels(env, bitmap);
        AndroidBitmap_unlockPixels(env, newBitmap);
        return newBitmap;
    }
}




