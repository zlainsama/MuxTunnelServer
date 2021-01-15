package me.lain.muxtun.mipo.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketAddressAdapter extends TypeAdapter<SocketAddress> {

    @Override
    public void write(JsonWriter out, SocketAddress value) throws IOException {
        if (value == null)
            out.nullValue();
        else if (value instanceof InetSocketAddress)
            writeInetSocketAddress(out, (InetSocketAddress) value);
        else
            throw new UnsupportedOperationException();
    }

    private void writeInetSocketAddress(JsonWriter out, InetSocketAddress value) throws IOException {
        out.value(value.getHostString() + ":" + value.getPort());
    }

    @Override
    public SocketAddress read(JsonReader in) throws IOException {
        String value = in.nextString();
        int indexToColon = value.lastIndexOf(":");
        String host = value.substring(0, indexToColon);
        int port = Integer.parseInt(value.substring(indexToColon + 1));
        return new InetSocketAddress(host, port);
    }

}
