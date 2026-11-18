# Keep VideoEditorActivity and methods invoked via reflection/JNI from libconcat_android.so
-keep class com.mosamircs.videoeditor.VideoEditorActivity { *; }
-keep class com.mosamircs.videoeditor.ConcatFiles { *; }
