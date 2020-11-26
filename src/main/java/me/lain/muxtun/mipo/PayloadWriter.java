package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
interface PayloadWriter {

    FastThreadLocal<List<ByteBuf>> SLICES_LIST = new FastThreadLocal<List<ByteBuf>>() {

        @Override
        protected List<ByteBuf> initialValue() throws Exception {
            return new ArrayList<>();
        }

    };

    static List<ByteBuf> slices(ByteBuf in, int size, List<ByteBuf> list) {
        if (list == null)
            list = SLICES_LIST.get();

        int length = in.readableBytes();
        if (length > size) {
            for (; ; ) {
                if (length > size) {
                    list.add(in.readSlice(size));
                    length -= size;
                } else {
                    list.add(in.readSlice(length));
                    break;
                }
            }
        } else {
            list.add(in);
        }

        return list;
    }

    boolean write(ByteBuf payload) throws Exception;

    default boolean writeSlices(ByteBuf payload) throws Exception {
        int length = payload.readableBytes();
        if (length >= 65536)
            return writeSlices(payload, 32768, null);
        else if (length >= 16384)
            return writeSlices(payload, 16384, null);
        else
            return writeSlices(payload, 8192, null);
    }

    default boolean writeSlices(ByteBuf payload, int size, List<ByteBuf> list) throws Exception {
        try {
            for (ByteBuf slice : (list = slices(payload, size, list))) {
                try {
                    if (!write(slice.retain(2)))
                        return false;
                } finally {
                    ReferenceCountUtil.release(slice);
                }
            }
        } finally {
            ReferenceCountUtil.release(payload);
            if (list != null)
                list.clear();
        }

        return true;
    }

}
