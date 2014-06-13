package net.geecode.framework.lite;

public interface IBehavior
{

    void attach(Object $component);
    void detach(Object $component);
    boolean getEnabled();
    void setEnabled(boolean $value);
}
