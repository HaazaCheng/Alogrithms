package com.hazza.algorithms.tree.LSM;

import com.hazza.algorithms.dataStructure.skiplist.SkipList;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Created with IntelliJ IDEA.
 * Description: A simple implementation for LSM Tree.
 * User: HazzaCheng
 * Contact: hazzacheng@gmail.com
 * Date: 18-8-4
 * Time: 2:30 PM
 */
public class LSMTree {
    /**
     * Split of records' attributes.
     */
    private static final String ATTRIBUTE_SPLIT = ",";
    /**
     * Split of records' location.
     */
    private static final String POSITION_SPLIT = "@";
    /**
     * The path of saving .sst files.
     */
    private String path;
    /**
     * The maximum size of .sst file.
     */
    private int threadshold;
    /**
     * The number of existing .sst files.
     */
    private int fileNums;
    /**
     * The skip table.
     */
    private SkipList<KVTuple> table;


    public LSMTree(int threadshold, String path) {
        this.threadshold = threadshold;
        this.path = path;
        fileNums = getSstFileNum();
        table = new SkipList<>(false);
    }

    /**
     * Get the number of sst files under the specified path.
     *
     * @return The number of sst files.
     */
    private int getSstFileNum() {
        File files = new File(path);

        int max = 0;
        for (File file : files.listFiles()) {
            if (file.isFile()) {
                int num = Integer.parseInt(file.getName().replace(".sst", ""));
                max = Math.max(num, max);
            }
        }

        return max;
    }

    public String get(String key) {
        Block block = getRecord(key);
        if (block != null && block.crc == getCrc(block.time, block.keySize, block.valueSize, block.key, block.value)) {
            if (block.value != null) {
                return block.value;
            }
        }

        return null;
    }

    public void put(String key, String value) {
        int keySize = key.length(), valueSize = 0;

        if (value != null) {
            valueSize = value.length();
        }

        long time = System.currentTimeMillis();
        long crc = getCrc(time, keySize, valueSize, key, value);

        String infos = ioWrite(crc, time, keySize, valueSize, key, value);
        KVTuple kv = new KVTuple(key, null);
        if (table.isFound(kv) != null) {
            table.delete(kv);
        }
        table.insert(new KVTuple(key, infos));
    }

    public void update(String key, String value) {
        put(key, value);
    }

    public void delete(String key) {
        put(key, null);
    }

    /**
     * Calculate the crc.
     *
     * @param time
     * @param keySize
     * @param valueSize
     * @param key
     * @param value
     * @return The crc.
     */
    private long getCrc(long time, int keySize, int valueSize, String key, String value) {
        CRC32 crc32 = new CRC32();
        String str = time + " " + keySize + " " + valueSize + " " + key + " " + value;
        crc32.update(str.getBytes());
        long crc = crc32.getValue();

        return crc;
    }

    /**
     * Get record.
     *
     * @param key The key.
     * @return The blcok.
     */
    private Block getRecord(String key) {
        KVTuple infos = table.get(new KVTuple(key, null));

        if (infos == null || infos.value == null) {
            return getRecordFromFiles(key);
        }

        String[] values = infos.value.split(POSITION_SPLIT);
        Block block = ioRead(values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2]));

        return block;
    }

    /**
     * Get record from all sst files.
     *
     * @param key
     * @return
     */
    private Block getRecordFromFiles(String key) {
        List<Integer> fileNums = new ArrayList<>();
        File files = new File(path);
        for (File file : files.listFiles()) {
            if (file.isFile()) {
                fileNums.add(Integer.parseInt(file.getName().replace(".sst", "")));
            }
        }

        Collections.sort(fileNums);
        int n = fileNums.size();
        for (int i = n - 1; i >= 0; i--) {
            String fileName = path + "/" + fileNums.get(i) + ".sst";
            Block block = getRecordFromFile(key, fileName);
            if (block != null) {
                return block;
            }
        }

        return null;
    }

    /**
     * Get record from the specified sst file.
     *
     * @param key
     * @param fileName
     * @return
     */
    private Block getRecordFromFile(String key, String fileName) {
        File file = new File(fileName);
        List<Block> res = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] attrs = line.split(ATTRIBUTE_SPLIT);
                if (attrs[4].equals(key)) {
                    res.add(new Block(Long.parseLong(attrs[0]), Long.parseLong(attrs[1]), Integer.parseInt(attrs[2]), Integer.parseInt(attrs[3]), attrs[4], attrs[5]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (res.size() > 0) {
            return res.get(res.size() - 1);
        }

        return null;
    }

    /**
     * Write into sst file.
     *
     * @param crc
     * @param time
     * @param keySize
     * @param valueSize
     * @param key
     * @param value
     * @return The location of the key.
     */
    private String ioWrite(long crc, long time, int keySize, int valueSize, String key, String value) {
        String curFile = path + "/" + fileNums + ".sst";
        File file = new File(curFile);

        if (file.length() >= threadshold) {
            ++fileNums;
            curFile = path + "/" + fileNums + ".sst";
            file = new File(curFile);
        }

        Block block = new Block(crc, time, keySize, valueSize, key, value);
        byte[] bytes = block.getBytes(ATTRIBUTE_SPLIT);
        int len = bytes.length;
        int start = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            start = (int) raf.length();
            raf.seek(start);
            raf.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return curFile + POSITION_SPLIT + start + POSITION_SPLIT + (len- Block.BLOCK_SPLIT_BYTE_SIZE);
    }

    /**
     * Read from sst file with specified position.
     *
     * @param fileName The sst file.
     * @param start    The start of the block.
     * @param length   The length of the block.
     * @return The block.
     */
    private Block ioRead(String fileName, int start, int length) {
        File file = new File(fileName);
        byte[] bytes = new byte[length  * 10];
        Block block = null;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            raf.read(bytes, start, length);
            block = Block.rcoverFromBytes(bytes, ATTRIBUTE_SPLIT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return block;
    }

    /**
     * Print all key-value stored in sst files.
     * @param filePath
     */
    public static void listAllKeyValueFromFiles(String filePath) {
        File files = new File(filePath);
        int cnt = 0;

        for (File file : files.listFiles()) {
            if (file.isFile() && file.getName().contains(".sst")) {
                System.out.println(file.getName() + " contains: ");
                String line;
                try (BufferedReader br = new BufferedReader(new FileReader(file))){
                    while ((line = br.readLine()) != null) {
                        String[] attrs = line.split(ATTRIBUTE_SPLIT);
                        if (attrs.length == 6) {
                            System.out.println("[key: " + attrs[4] + ", value: " + attrs[5] + ", time: " + attrs[1] + "]");
                            ++cnt;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println();
            }
        }

        System.out.println("\nTotally have " + cnt + " key-values.");
    }
}
