package com.AdamNew.dynamicwaters;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import net.minecraft.client.renderer.PanoramaRenderer;
public class TestFields {
    public static void print() {
        System.out.println("========== PANORAMA RENDERER METHODS ==========");
        for (Method m : PanoramaRenderer.class.getDeclaredMethods()) {
            System.out.print(m.getName() + "(");
            for (Class<?> p : m.getParameterTypes()) {
                System.out.print(p.getName() + ", ");
            }
            System.out.println(")");
        }
        System.out.println("========== PANORAMA RENDERER FIELDS ==========");
        for (Field f : PanoramaRenderer.class.getDeclaredFields()) {
            System.out.println(f.getName() + " : " + f.getType().getName());
        }
    }
}
