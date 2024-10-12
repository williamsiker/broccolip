#include <jni.h>
#include <string>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <android/bitmap.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/NeuralNetworks.h>
#include <cmath>
#include <curl/curl.h>
#include <algorithm>
#include <sstream>

// Función para escribir datos recibidos en un buffer
size_t WriteCallback(void *contents, size_t size, size_t nmemb, std::string *output) {
    size_t totalSize = size * nmemb;
    output->append((char *) contents, totalSize);
    return totalSize;
}

// Función para cargar el certificado PEM desde los assets
std::string loadPemFromRaw(JNIEnv* env, jobject context, int rawResId) {
    // Obtener la instancia de AssetManager
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getAssetsMethod = env->GetMethodID(contextClass, "getAssets", "()Landroid/content/res/AssetManager;");
    jobject assetManager = env->CallObjectMethod(context, getAssetsMethod);

    // Abrir el recurso raw
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    AAsset* asset = AAssetManager_open(mgr, "cacert.pem", AASSET_MODE_BUFFER);
    if (!asset) {
        return "";
    }

    // Leer el archivo
    off_t size = AAsset_getLength(asset);
    std::string pemData(size, '\0');
    AAsset_read(asset, &pemData[0], size);
    AAsset_close(asset);

    return pemData;
}

extern "C" {
    /*Test connection*/
    JNIEXPORT jboolean JNICALL
    Java_com_example_testapp_filecnt_FileViewModel_nativeTestConnection(JNIEnv *env, jobject instance, jstring jFtpUrl){
        const char *ftpUrl = env->GetStringUTFChars(jFtpUrl, nullptr);
        if(!ftpUrl) return JNI_FALSE;

        CURL *curl;
        CURLcode res = CURLE_OK;
        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();

        if (curl) {
            curl_easy_setopt(curl, CURLOPT_URL, ftpUrl);

            curl_easy_cleanup(curl);
            if (res != CURLE_OK) {
                env->ReleaseStringUTFChars(jFtpUrl, ftpUrl);
                curl_global_cleanup();
                return JNI_FALSE;
            }
        }

        env->ReleaseStringUTFChars(jFtpUrl, ftpUrl);
        curl_global_cleanup();
        return JNI_TRUE;
    }
    /*Fin*/
    /* FTP UPLOAD FUNCTION */
    JNIEXPORT jboolean JNICALL
    Java_com_example_testapp_filecnt_FileViewModel_uploadFileToFTP(
            JNIEnv *env,
            jobject instance,
            jstring jFtpUrl,
            jstring jFilepath
    ) {
        const char *ftpUrl = env->GetStringUTFChars(jFtpUrl, nullptr);
        const char *filePath = env->GetStringUTFChars(jFilepath, nullptr);

        CURL *curl;
        CURLcode res = CURLE_OK;
        FILE *fp;

        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();

        if (curl) {
            fp = fopen(filePath, "rb");
            if (fp) {
                curl_easy_setopt(curl, CURLOPT_FTP_USE_EPSV, 1L);
                curl_easy_setopt(curl, CURLOPT_READDATA, fp);
                curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
                curl_easy_setopt(curl, CURLOPT_URL, ftpUrl);

                res = curl_easy_perform(curl);

                if (res != CURLE_OK) {
                    // Log the error message
                    fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
                }

                fclose(fp);
            } else {
                fprintf(stderr, "Failed to open file: %s\n", filePath);
                res = CURLE_READ_ERROR;
            }

            curl_easy_cleanup(curl);
        } else {
            fprintf(stderr, "Failed to initialize curl\n");
            res = CURLE_FAILED_INIT;
        }

        env->ReleaseStringUTFChars(jFtpUrl, ftpUrl);
        env->ReleaseStringUTFChars(jFilepath, filePath);
        curl_global_cleanup();

        return res == CURLE_OK ? JNI_TRUE : JNI_FALSE;
    }

    //2 XDDDD
    JNIEXPORT jboolean JNICALL
    Java_com_example_testapp_filecnt_FileViewModel_uploadChunkstoFTP(
            JNIEnv *env,
            jobject instance,
            jobjectArray jFtpUrls,
            jobjectArray jFilepaths
            ) {
        jsize fileCount = env->GetArrayLength(jFilepaths);
        jsize curlCount = env->GetArrayLength(jFtpUrls);
        if (fileCount == 0 || fileCount != curlCount) {
            return JNI_FALSE;
        }

        CURL *curl;
        CURLcode res = CURLE_OK;
        FILE *fp;

        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();

        if (curl) {
            for (jsize i = 0; i < fileCount; ++i) {
                auto jFilepath = (jstring) env->GetObjectArrayElement(jFilepaths, i);
                const char *filePath = env->GetStringUTFChars(jFilepath, nullptr);

                auto jFileUrl = (jstring) env->GetObjectArrayElement(jFtpUrls, i);
                const char *ftpUrl = env->GetStringUTFChars(jFileUrl, nullptr);
                if (!filePath || !ftpUrl) {
                    continue; // Skip this file if path is null
                }

                fp = fopen(filePath, "rb");
                if (fp) {
                    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
                    curl_easy_setopt(curl, CURLOPT_READDATA, fp);
                    curl_easy_setopt(curl, CURLOPT_URL, ftpUrl);

                    res = curl_easy_perform(curl);

                    fclose(fp);
                    fp = nullptr;
                }

                env->ReleaseStringUTFChars(jFilepath, filePath);
                env->ReleaseStringUTFChars(jFileUrl, ftpUrl);

                if (res != CURLE_OK) {
                    break; // Exit on the first failure
                }
            }

            curl_easy_cleanup(curl);
        }

        curl_global_cleanup();

        return (res == CURLE_OK) ? JNI_TRUE : JNI_FALSE;
    }
    /* END FTP FUNCTION */

    // Función para obtener contenido de una página web
    JNIEXPORT jstring JNICALL
    Java_com_example_testapp_filecnt_FileViewModel_fetchPageContent(JNIEnv *env, jobject obj, jint rawResId) {
        CURL *curl;
        CURLcode res;
        std::string readBuffer;
        std::string url = "https://jsonplaceholder.typicode.com/posts/1";

        std::string pemData = loadPemFromRaw(env, obj, rawResId);
        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();

        if (curl) {
            curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
            curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);
            curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);
            curl_easy_setopt(curl, CURLOPT_CAINFO, pemData.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
            curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
            res = curl_easy_perform(curl);
            if (res != CURLE_OK) {
                curl_easy_cleanup(curl);
                curl_global_cleanup();
                return env->NewStringUTF("no se pudo conectar");
            }
            curl_easy_cleanup(curl);
        }

        curl_global_cleanup();
        return env->NewStringUTF(readBuffer.c_str());
    }
    /* FIN */

    // Función para conectar a un servidor usando sockets
    JNIEXPORT jboolean JNICALL
    Java_com_example_testapp_camerax_CameraViewModel_nativeConnectToServer(JNIEnv *env, jobject thiz,
                                                                   jstring ip, jint port) {
        const char *ipStr = env->GetStringUTFChars(ip, nullptr);
        int serverSocket = socket(AF_INET, SOCK_STREAM, 0);
        if (serverSocket < 0) {
            return JNI_FALSE;
        }

        struct sockaddr_in serverAddress{};
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(port);
        serverAddress.sin_addr.s_addr = inet_addr(ipStr);

        int connectResult = connect(serverSocket, (struct sockaddr *) &serverAddress,
                                    sizeof(serverAddress));
        env->ReleaseStringUTFChars(ip, ipStr);

        if (connectResult < 0) {
            close(serverSocket);
            return JNI_FALSE;
        }

        //close(serverSocket);
        return JNI_TRUE;
    }
    /* END */

}
