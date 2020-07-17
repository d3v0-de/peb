package de.d3v0.peb.common.misc;

public class Utils {
    public static boolean isNullOrEmtpy(String value)
    {
        return value == null || value.length() == 0;
    }

    public static String humanReadable(long size)
    {
        return humanReadable(size, true);
    }

    public static String humanReadable(long size, boolean extended)
    {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double curSize = size;
        String res= null;

        for (int i= 0; i < units.length - 1; i++)
        {
            if (curSize / 1024 > 1)
                curSize = curSize / 1024;
            else {
                res = String.format("%.2f " + units[i], curSize);
                break;
            }
        }
        if (res == null)
            res =  String.format("%.2f " + units[units.length - 1], curSize);

        if (extended)
            res = String.format("%s (%s B)", res, thousands(size));

        return res;
    }

    private static String thousands(long num)
    {
        long cur = num;
        StringBuilder s = new StringBuilder();
        while(cur > 0)
        {
            s.insert(0,cur % 1000);
            if (cur > 1000)
                s.insert(0, ".");
            cur = cur / 1000;
        }
        return s.toString();
    }


}
