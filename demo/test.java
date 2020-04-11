package com.sort;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @Author: Chensungw
 * @Date: 2018-12-04 15:28
 **/
public class test {

    private static final int numBlock = 24; //磁盘总的块数
    private static final int numRecord = 2; //每个块的记录数
    private static final int numCache = 3;  //缓冲区大小
    private static final List<Block> blockList = new ArrayList<>();

    public static void main(String[] args) {
        //生成磁盘
        System.out.println("生成随机磁盘1：");
        random(false,"random1.dat");
        System.out.println("生成随机磁盘2：");
        random(true,"random2.dat");
        System.out.println();

        //初始化归并段，即外排序按照缓冲区大小的磁盘块
        System.out.print("初始化归并段1：");
        initMerging("random1.dat","init1.dat",numCache);
        System.out.print("\n\n初始化归并段2：");
        initMerging("random2.dat","init2.dat",numCache);

        //归并排序
        System.out.print("\n\n-----归并排序文件1-------");
        int resultFileNum1 = mergingSortCtr("init1.dat", "merging11.dat", "merging12.dat");
        System.out.print("\n\n-----归并排序文件2-------");
        int resultFileNum2 = mergingSortCtr("init2.dat", "merging21.dat", "merging22.dat");

        //显示排好序的文件
        System.out.println();
        String file1,file2;
        if(resultFileNum1 == 1)
            file1 = "merging11.dat";
        else
            file1 = "merging12.dat";
        if(resultFileNum2 == 1)
            file2 = "merging21.dat";
        else
            file2 = "merging22.dat";
        System.out.println("\n-------------排好序的文件1----------------");
        display(file1);
        System.out.println();
        System.out.println("\n-------------排好序的文件2----------------");
        display(file2);
        System.out.println();

        //连接
        join(file1,file2);
    }

    /**
     * 连接函数
     *
     * @param file1：将要连接的文件1
     * @param file2：将要连接的文件2
     */
    private static void join(String file1,String file2){
        System.out.println("\n+++++++++++++++++连接结果++++++++++++++++++++++++");
        File file11 = new File(file1);
        File file22 = new File(file2);

        try {
            FileInputStream in1 = new FileInputStream(file11);
            FileInputStream in2 = new FileInputStream(file22);
            ObjectInputStream objIn1 = new ObjectInputStream(in1);
            ObjectInputStream objIn2 = new ObjectInputStream(in2);

            Block b1 =(Block) objIn1.readObject();
            Block b2 =(Block) objIn2.readObject();
            int num1 = 1,num2 = 1;
            int temp1,pr,ps,aa = 0,bb = 0;
            List<Integer> list = new ArrayList<>();
            while ((num2 < numBlock +1) && (num1 < numBlock + 1)){
                if(b1 == null)
                    break;
                pr = b1.getRecord()[aa];

                ps = temp1 = b2.getRecord()[bb];
                list.add(ps);
                bb++;
                if(bb == numRecord){
                    num2++;
                    b2 =(Block) objIn2.readObject();
                    bb = 0;
                }
                boolean done = true;
                while (done){
                    if(b2 == null)
                        break;
                    ps = b2.getRecord()[bb];
                    if(bb == numRecord){
                        num2++;
                        b2 =(Block) objIn2.readObject();
                        bb = 0;
                    }
                    if(ps == temp1){
                        list.add(ps);
                        bb++;
                        if(bb == numRecord){
                            num2++;
                            b2 =(Block) objIn2.readObject();
                            bb = 0;
                        }
                    }
                    else {

                        done = false;
                    }
                }

                int listSize = list.size();
                list.clear();
                while (pr < temp1){
                    aa++;

                    if(aa == numRecord){
                        num1++;

                        b1 = (Block)objIn1.readObject();
                        aa = 0;
                    }
                    if(b1 == null)
                        break;
                    pr = b1.getRecord()[aa];
                }
                while (pr == temp1){
                    for (int i = 0; i < listSize; i++) {
                        System.out.println("pr : " + pr + " 连接 " + "ps : " + temp1 );
                    }
                    System.out.println();
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * 归并排序函数
     *
     * @param initF
     * @param mF1
     * @param mF2
     */
    private static int mergingSortCtr(String initF, String mF1, String mF2){
        //计算趟数,归并排序
        int mergingNumber = (int) Math.ceil((Math.log((double) numBlock / numCache)) / (Math.log(numCache - 1)));
        int skipBlock = numCache;
        int segment = numBlock / (numCache * 2);
        int fileNumber = 1;
        for (int i = 0; i < mergingNumber; i++) {
            if(i == 0)
                mergingSort(initF, mF1,segment,skipBlock);
            else{
                if(fileNumber == 1){
                    mergingSort(mF1, mF2,segment,skipBlock);
                    fileNumber = 2;
                }else {
                    mergingSort(mF2, mF1,segment,skipBlock);
                    fileNumber = 1;
                }
            }
            skipBlock *= 2;
            segment /= 2;
        }
        return fileNumber;
    }
    /**
     * 将磁盘块写入文件
     *
     * @param list：磁盘块列表
     * @param fileName：写入文件的文件名
     * @param extend：是否追加写入磁盘
     * @param isNewFile:判断是新写入数据，还是追加写入数据
     */
    private static void writeObjectToFile(List<Block> list, String fileName, boolean extend,boolean isNewFile) {

        File file = new File(fileName);
        ObjectOutputStream objOut = null;
        try {
            FileOutputStream out = new FileOutputStream(file, extend);
            if(isNewFile)
                 objOut = new ObjectOutputStream(out);
            else
                objOut =new MyObjectOutputStream(out);
            if(list != null){
                for (Block block : list) {
                    objOut.writeObject(block);
                    objOut.flush();
                }
            }else {
                objOut.writeObject(null);
            }


            objOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化创建初始化归并段
     *
     * @param randomName:随机文件名
     * @param initFile:存放初始化归并段的结果的文件名
     * @param numCache：缓冲区大小
     */
    public static void initMerging(String randomName,String initFile,int numCache) {
        List<Block> temp = new ArrayList<>();
        Boolean bool = false;
        File file = new File(randomName);

        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(in);
            List<Block> bb = null;
            for (int j = 0; j < numBlock / numCache; j++) {
                for (int i = 0; i < numCache; i++) {
                    Block g = (Block) objIn.readObject();
                    temp.add(g);
                    bb = sort(temp);
                }
                if(j == 0)
                    writeObjectToFile(bb, initFile, bool,true);
                else
                    writeObjectToFile(bb, initFile, bool,false);
                bool = true;
                bb.clear();

                System.out.println();
                display(initFile);
            }
            writeObjectToFile(null, initFile, true, false);
//            System.out.println();
            objIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 归并排序
     *
     * @param initFileName:存放初始化归并段的文件名
     * @param mergingFileName:存放归并排序后的文件名
     * @param mergingNumber:归并排序的段数
     */
    private static void mergingSort(String initFileName,String mergingFileName,int mergingNumber,int skipBlock) {

        File initFile = new File(initFileName);
        File mergingFile = new File(mergingFileName);

        try {
            FileInputStream in1 = new FileInputStream(initFile);
            FileInputStream in2 = new FileInputStream(initFile);
            FileOutputStream mergingIn = new FileOutputStream(mergingFile);

            ObjectInputStream objIn1 = new ObjectInputStream(in1);
            ObjectInputStream objIn2 = new ObjectInputStream(in2);
            ObjectOutputStream objMergingOut = new ObjectOutputStream(mergingIn);
            int kk = 0;
            for (int i1 = 0; i1 < mergingNumber; i1++) {
                Block b1 ;
                Block b2 ;
                int aNo = 1, bNo = 1, aR, bR, aa = 0, bb = 0, cc = 0;
                b1 = (Block) objIn1.readObject();

                for (int i2 = 0; i2 < skipBlock - kk; i2++) {
                    objIn2.readObject();
                }
                kk = 1;
                b2 = (Block) objIn2.readObject();
                Block block = new Block(numRecord);
                while ((aNo < numRecord * skipBlock + 1) && (bNo < numRecord * skipBlock + 1)) {
                    aR = b1.getRecord()[aa];
                    bR = b2.getRecord()[bb];
                    if (aR < bR) {
                        block.getRecord()[cc++] = aR;
                        if (++aa == numRecord) {
                            b1 = (Block) objIn1.readObject();
                            aa = 0;
                        }
                        aNo++;
                    } else {
                        block.getRecord()[cc++] = bR;
                        if (++bb == numRecord) {
                            bb = 0;
                            b2 = (Block) objIn2.readObject();
                        }
                        bNo++;
                    }
                    if (cc == numRecord) {
                        Block temp = new Block(numRecord);
                        cc = 0;
                        for (int i = 0; i < numRecord; i++) {
                            temp.getRecord()[i] = block.getRecord()[i];
                        }
                        objMergingOut.writeObject(temp);
                    }
                }
                if (aNo < numRecord * skipBlock + 1) {
                    for (int j = aNo; j < numRecord * skipBlock + 1; j++) {
                        block.getRecord()[cc++] = b1.getRecord()[aa++];
                        if (aa == numRecord) {
                            b1 = (Block) objIn1.readObject();
                            aa = 0;
                        }
                        if (cc == numRecord) {
                            cc = 0;
                            Block temp = new Block(numRecord);
                            for (int i3 = 0; i3 < numRecord; i3++) {
                                temp.getRecord()[i3] = block.getRecord()[i3];
                            }
                            objMergingOut.writeObject(temp);
                        }

                    }
                } else {
                    for (int jj = bNo; jj < numRecord * skipBlock + 1; jj++) {
                        block.getRecord()[cc++] = b2.getRecord()[bb++];
                        if (bb == numRecord) {
                            bb = 0;
                            b2 = (Block) objIn2.readObject();
                        }
                        if (cc == numRecord) {
                            Block temp = new Block(numRecord);
                            for (int i = 0; i < numRecord; i++) {
                                temp.getRecord()[i] = block.getRecord()[i];
                            }
                            cc = 0;
                            objMergingOut.writeObject(temp);
                        }

                    }
                }

                for (int i4 = 0; i4 < skipBlock-1; i4++) {
                    objIn1.readObject();
                }
                System.out.println();
                display(mergingFileName);
            }
            objMergingOut.writeObject(null);
            objIn1.close();
            objIn2.close();
            objMergingOut.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.print("\n");
    }

    /**
     * 生成随机磁盘块
     *
     * @param rand:随机数
     * @param fileName:生成的结果放到文件的名字
     */
    public static void random(boolean rand,String fileName) {
        if(rand == true)
            blockList.clear();
        for (int i = 0; i < numBlock; i++) {
            if(rand == true)
                blockList.add(new Block(rand,numRecord));
            else
                blockList.add(new Block(numRecord));
        }

        writeObjectToFile(blockList, fileName, false,true);
        for (Block block : blockList) {
            System.out.print(block.toString());
        }
        System.out.println();
    }

    /**
     * 对缓冲区排序
     *
     * @param b：缓冲区块列表
     * @return ：排好序的磁盘块列表
     */
    private static List<Block> sort(List<Block> b) {
        int[] sortResult = new int[b.size() * numRecord];
        int k = 0;
        for (Block block : b) {
            for (int i = 0; i < numRecord; i++) {
                sortResult[k] = block.getRecord()[i];
                k++;
            }
        }
        k = 0;
        Arrays.sort(sortResult);
        for (Block block : b) {
            for (int i = 0; i < b.get(0).value; i++) {
                block.getRecord()[i] = sortResult[k];
                k++;
            }
        }
        return b;
    }

    /**
     * 显示文件
     *
     * @param file：要显示文件的文件名
     */
    private static void display(String file) {
        File file1 = new File(file);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while (true) {
                for (int i = 0; i < numCache; i++) {
                    Block block = (Block) ois.readObject();
                    if(block != null)
                        System.out.print(block.toString());
                }

            }
        } catch (EOFException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

/**
 * 覆写父类中的方法，使他调用writeObject()的时候不写入文件头
 */
class MyObjectOutputStream extends ObjectOutputStream{

    public MyObjectOutputStream() throws IOException {
        super();
    }
    public MyObjectOutputStream(OutputStream o) throws IOException {
        super(o);
    }
    @Override
    public void writeStreamHeader(){}
}

/**
 *  磁盘块数据结构
 */
class Block implements Serializable {
    int []record;
    int value;

    Block(boolean rand,int value) {
        this.value = value;
        record = new int[value];
        Random random = new Random();
        for (int i = value-1; i >= 0; i--) {
//            record[i] = (random.nextInt(99));
            record[i] = (int)(random.nextDouble()*100);
        }
    }
    Block(int value) {
        this.value = value;
        record = new int[value];
        Random random = new Random();
        for (int i = value-1; i >= 0; i--) {
            record[i] = random.nextInt(80);
        }
    }

    public int[] getRecord() {
        return record;
    }

    @Override
    public String toString() {
        return Arrays.toString(record);
    }
}
