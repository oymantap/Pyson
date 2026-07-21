#include <jni.h>
#include <string>
#include <sstream>
#include <vector>
#include <unordered_set>

// Set keyword Python untuk parsing kilat di C++
const std::unordered_set<std::string> PYTHON_KEYWORDS = {
    "def", "return", "if", "elif", "else", "while", "for", "in", 
    "import", "from", "as", "class", "try", "except", "lambda", 
    "with", "pass", "break", "continue", "True", "False", "None"
};

extern "C" JNIEXPORT jstring JNICALL
Java_com_pyson_NativeEngine_analyzeCodeFast(JNIEnv *env, jobject /* this */, jstring code) {
    const char *nativeCode = env->GetStringUTFChars(code, nullptr);
    if (!nativeCode) return env->NewStringUTF("");

    std::string input(nativeCode);
    env->ReleaseStringUTFChars(code, nativeCode);

    // Contoh fast processing: Menghitung statistik / tokenizing langsung dari C++ memory
    std::istringstream stream(input);
    std::string line;
    int lineCount = 0;
    int kwCount = 0;

    while (std::getline(stream, line)) {
        lineCount++;
        std::istringstream lineStream(line);
        std::string word;
        while (lineStream >> word) {
            if (PYTHON_KEYWORDS.find(word) != PYTHON_KEYWORDS.end()) {
                kwCount++;
            }
        }
    }

    std::string result = "Lines: " + std::to_string(lineCount) + " | Keywords: " + std::to_string(kwCount);
    return env->NewStringUTF(result.c_str());
}
