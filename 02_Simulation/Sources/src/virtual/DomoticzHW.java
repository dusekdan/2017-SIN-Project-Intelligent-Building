/*
 * Created by - on 1.12.2017.
 *
 * SIN Project 2017 - Intelligent Building Simulation
 * Faculty of Information Technology, Brno University of Technology
 */

public class DomoticzHW
{
    private int idx;
    private String name;
    private String data;

    public DomoticzHW(int idx, String name, String data)
    {
        this.idx = idx;
        this.name = name;
        this.data = data;
    }

    /*
     * Getters & Setters
     */

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public int getIdx()
    {
        return idx;
    }

    public void setIdx(int idx)
    {
        this.idx = idx;
    }
}
