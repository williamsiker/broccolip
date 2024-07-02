#include <jni.h>
#include <string>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <android/bitmap.h>
#include <cmath>
#include <curl/curl.h>

size_t WriteCallback(void* contents, size_t size, size_t nmemb, std::string* output) {
    size_t totalSize = size * nmemb;
    output->append((char*)contents, totalSize);
    return totalSize;
}

extern "C" {
    //Function PET
    JNIEXPORT jstring JNICALL
    Java_com_example_amogusapp_SendFilesActivity_fetchPageContent(JNIEnv* env, jobject) {
        CURL* curl;
        CURLcode res;
        std::string readBuffer;
        std::string url = "https://jsonplaceholder.typicode.com/posts/1";

        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();

        if (curl) {
            curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
            res = curl_easy_perform(curl);
            if (res != CURLE_OK) {
                fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
            }else{
                return env->NewStringUTF("amogus");
            }
            curl_easy_cleanup(curl);
        }

        curl_global_cleanup();
        return env->NewStringUTF(readBuffer.c_str());
    }
    /*FIN*/

    JNIEXPORT jobject JNICALL
    Java_com_example_amogusapp_CameraHandler_nativeRotateAndScale(
            JNIEnv *env,
            jobject instance,
            jobject bitmap,
            jfloat rotationDegrees
    ) {
        AndroidBitmapInfo info;
        void* pixels;

        if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
            return NULL;
        }

        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
            return NULL;
        }

        int newWidth, newHeight;
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            newWidth = info.height;
            newHeight = info.width;
        } else {
            newWidth = info.width;
            newHeight = info.height;
        }

        jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
        jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        jstring configName = env->NewStringUTF("ARGB_8888");
        jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
        jmethodID valueOfConfigMethod = env->GetStaticMethodID(configClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
        jobject bitmapConfig = env->CallStaticObjectMethod(configClass, valueOfConfigMethod, configName);

        jobject newBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, newWidth, newHeight, bitmapConfig);

        void* newPixels;
        if (AndroidBitmap_lockPixels(env, newBitmap, &newPixels) < 0) {
            AndroidBitmap_unlockPixels(env, bitmap);
            return NULL;
        }

        uint32_t* src = (uint32_t*)pixels;
        uint32_t* dst = (uint32_t*)newPixels;

        for (int y = 0; y < info.height; ++y) {
            for (int x = 0; x < info.width; ++x) {
                int newX, newY;
                switch ((int)rotationDegrees) {
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

    //Conexion a Sockets con C++
    JNIEXPORT jboolean JNICALL
    Java_com_example_amogusapp_CameraHandler_nativeConnectToServer(JNIEnv *env, jobject thiz, jstring ip, jint port) {
        const char *ipStr = env->GetStringUTFChars(ip, nullptr);
        int serverSocket = socket(AF_INET, SOCK_STREAM, 0);
        if (serverSocket < 0) {
            return JNI_FALSE;
        }

        struct sockaddr_in serverAddress;
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(port);
        serverAddress.sin_addr.s_addr = inet_addr(ipStr);

        int connectResult = connect(serverSocket, (struct sockaddr *) &serverAddress, sizeof(serverAddress));
        env->ReleaseStringUTFChars(ip, ipStr);

        if (connectResult < 0) {
            close(serverSocket);
            return JNI_FALSE;
        }

        //close(serverSocket);
        return JNI_TRUE;
    }
}




