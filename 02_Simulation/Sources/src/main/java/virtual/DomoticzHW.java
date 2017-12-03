/*
 * Created by - on 1.12.2017.
 *
 * SIN Project 2017 - Intelligent Building Simulation
 * Faculty of Information Technology, Brno University of Technology
 */

package virtual;

public class DomoticzHW
{
    private String idx;
    private String name;
    private String data;

    private boolean isHW;

    public DomoticzHW(String idx, String name, String data, boolean isHardware)
    {
        this.idx = idx;
        this.name = name;
        this.data = data;
        this.isHW = isHardware;
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

    public String getIdx()
    {
        return idx;
    }

    public void setIdx(String idx)
    {
        this.idx = idx;
    }
}
