package com.AdamNew.dynamicwaters.client.gui;

//? if <1.20.5 {
/*import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class DWSubScreen extends DWScreen {
    private static final int FOOTER = 33;

    protected final Screen lastScreen;
    protected Rows list;
    protected int headerHeight = 33;

    protected DWSubScreen(Screen lastScreen, Component title) {
        super(title);
        this.lastScreen = lastScreen;
    }

    protected abstract void addOptions();

    protected void addHeader() {
    }

    @Override
    protected void init() {
        addHeader();
        this.list = new Rows(this.minecraft, this.width, this.height, this.headerHeight, this.height - FOOTER);
        addOptions();
        addRenderableWidget(this.list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    public static final class Rows extends ContainerObjectSelectionList<Rows.Row> {
        Rows(Minecraft minecraft, int width, int height, int top, int bottom) {
            super(minecraft, width, height, top, bottom, 25);
            this.centerListVertically = false;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        public void addSmall(AbstractWidget left, AbstractWidget right) {
            addEntry(new Row(left, right));
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 165;
        }

        public static final class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final List<AbstractWidget> widgets = new ArrayList<>();

            Row(AbstractWidget left, AbstractWidget right) {
                if (left != null) this.widgets.add(left);
                if (right != null) this.widgets.add(right);
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int x = left;
                for (AbstractWidget widget : this.widgets) {
                    widget.setX(x);
                    widget.setY(top);
                    widget.render(graphics, mouseX, mouseY, partialTick);
                    x += 160;
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return this.widgets;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return this.widgets;
            }
        }
    }
}
*///?}
