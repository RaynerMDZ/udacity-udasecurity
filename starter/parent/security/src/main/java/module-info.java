module security {
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires image;
    requires com.miglayout.swing;
    requires java.sql;
    opens com.udacity.security.data to com.google.gson;
}