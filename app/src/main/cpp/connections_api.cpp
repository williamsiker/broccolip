#include <jni.h>
#include <string>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <android/bitmap.h>
#include <cmath>
#include <curl/curl.h>
#include <algorithm>
#include <sstream>

#include <vulkan/vulkan.h>

// Función para escribir datos recibidos en un buffer
size_t WriteCallback(void *contents, size_t size, size_t nmemb, std::string *output) {
    size_t totalSize = size * nmemb;
    output->append((char *) contents, totalSize);
    return totalSize;
}

extern "C" {
/* FTP UPLOAD FUNCTION */
JNIEXPORT jboolean JNICALL
Java_com_example_amogusapp_SendFilesActivity_uploadFileToFTP(
        JNIEnv *env,
        jobject instance,
        jstring jFtpUrl,
        jstring jFilepath
) {
    const char *ftpUrl = env->GetStringUTFChars(jFtpUrl, nullptr);
    const char *filePath = env->GetStringUTFChars(jFilepath, nullptr);

    if (!ftpUrl || !filePath) {
        return JNI_FALSE;
    }

    CURL *curl;
    CURLcode res = CURLE_OK;
    FILE *fp;

    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();

    if (curl) {
        fp = fopen(filePath, "rb");
        if (fp) {
            curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
            curl_easy_setopt(curl, CURLOPT_READDATA, fp);
            curl_easy_setopt(curl, CURLOPT_URL, ftpUrl);

            res = curl_easy_perform(curl);

            fclose(fp);
        }

        curl_easy_cleanup(curl);
        if (res != CURLE_OK) {
            env->ReleaseStringUTFChars(jFtpUrl, ftpUrl);
            env->ReleaseStringUTFChars(jFilepath, filePath);
            curl_global_cleanup();
            return JNI_FALSE;
        }
    }

    env->ReleaseStringUTFChars(jFtpUrl, ftpUrl);
    env->ReleaseStringUTFChars(jFilepath, filePath);
    curl_global_cleanup();
    return JNI_TRUE;
}
/* END FTP FUNCTION */

// Función para obtener contenido de una página web
JNIEXPORT jstring JNICALL
Java_com_example_amogusapp_SendFilesActivity_fetchPageContent(JNIEnv *env, jobject) {
    CURL *curl;
    CURLcode res;
    std::string readBuffer;
    std::string url = "https://jsonplaceholder.typicode.com/posts/1";

    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();

    if (curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
        curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
        res = curl_easy_perform(curl);
        if (res != CURLE_OK) {
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
Java_com_example_amogusapp_CameraHandler_nativeConnectToServer(JNIEnv *env, jobject thiz,
                                                               jstring ip, jint port) {
    const char *ipStr = env->GetStringUTFChars(ip, nullptr);
    int serverSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSocket < 0) {
        return JNI_FALSE;
    }

    struct sockaddr_in serverAddress;
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
