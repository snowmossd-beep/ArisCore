package me.vennlmao.ariscore.team.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InventorySerializer {

    public static String toBase64(ItemStack[] contents) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(out);
            data.writeInt(contents.length);
            for (ItemStack item : contents) data.writeObject(item);
            data.close();
            return Base64Coder.encodeLines(out.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack[] fromBase64(String base64, int size) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[size];
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream data = new BukkitObjectInputStream(in);
            int length = data.readInt();
            ItemStack[] contents = new ItemStack[size];
            for (int i = 0; i < length && i < size; i++) {
                contents[i] = (ItemStack) data.readObject();
            }
            data.close();
            return contents;
        } catch (ClassNotFoundException | IOException e) {
            return new ItemStack[size];
        }
    }
              }
                               
