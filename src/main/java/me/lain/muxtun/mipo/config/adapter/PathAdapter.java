package me.lain.muxtun.mipo.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathAdapter extends TypeAdapter<Path> {

    @Override
    public void write(JsonWriter out, Path value) throws IOException {
        if (value == null)
            out.nullValue();
        else
            out.value(value.toString());
    }

    @Override
    public Path read(JsonReader in) throws IOException {
        String value = in.nextString();
        return Paths.get(value);
    }

}
