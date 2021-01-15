package me.lain.muxtun.mipo.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class UUIDAdapter extends TypeAdapter<UUID> {

    @Override
    public void write(JsonWriter out, UUID value) throws IOException {
        if (value == null)
            out.nullValue();
        else
            out.value(value.toString());
    }

    @Override
    public UUID read(JsonReader in) throws IOException {
        String value = in.nextString();
        return UUID.fromString(value);
    }

}
