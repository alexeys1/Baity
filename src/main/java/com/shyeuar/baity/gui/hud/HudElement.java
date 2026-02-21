package com.shyeuar.baity.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface HudElement {
    String getId();
    
    String getDisplayName();
    
    double getX();
    
    double getY();
    
    void setX(double x);
    
    void setY(double y);
    
    float getScale();
    
    void setScale(float scale);
    
    double getDefaultX();
    
    double getDefaultY();
    
    float getDefaultScale();
    
    default void reset() {
        setX(getDefaultX());
        setY(getDefaultY());
        setScale(getDefaultScale());
    }
    
    boolean isSelected();
    
    void setSelected(boolean selected);
    
    default boolean isClicked() {
        return false;
    }
    
    default void setClicked(boolean clicked) {
    }
    
    default int getAbsX(int objWidth) {
        double currentX = getX();
        int screenWidth = com.shyeuar.baity.gui.hud.HudScreenUtils.getScaledWidth();
        
        int pixelX;
        boolean isRelative = currentX >= 0 && currentX <= 1.0;
        if (isRelative) {
            pixelX = (int)(currentX * screenWidth);
            pixelX -= objWidth / 2;
        } else {
            pixelX = (int)currentX;
        }
        
        return pixelX;
    }
    
    default int getAbsY(int objHeight) {
        double currentY = getY();
        int screenHeight = com.shyeuar.baity.gui.hud.HudScreenUtils.getScaledHeight();
        
        int pixelY;
        boolean isRelative = currentY >= 0 && currentY <= 1.0;
        if (isRelative) {
            pixelY = (int)(currentY * screenHeight);
            pixelY -= objHeight / 2;
        } else {
            pixelY = (int)currentY;
        }
        
        return pixelY;
    }
    
    default int getDummyWidth(boolean random) {
        if (random) return 5;
        return (int)(getWidth() * getScale());
    }
    
    default int getDummyHeight(boolean random) {
        if (random) return 5;
        return (int)(getHeight() * getScale());
    }
    
    int getWidth();
    
    int getHeight();
    
    default int moveX(int deltaX, int objWidth) {
        double currentX = getX();
        int screenWidth = com.shyeuar.baity.gui.hud.HudScreenUtils.getScaledWidth();
        
        int pixelX;
        boolean isRelative = currentX >= 0 && currentX <= 1.0;
        if (isRelative) {
            pixelX = (int)(currentX * screenWidth);
        } else {
            pixelX = (int)currentX;
        }
        
        int wasPositiveX = pixelX >= 0 ? 1 : 0;
        pixelX += deltaX;
        
        int newDeltaX = deltaX;
        if (wasPositiveX == 1) {
            if (pixelX < 0) {
                newDeltaX -= pixelX;
                pixelX = 0;
            } else if (pixelX > screenWidth) {
                newDeltaX += screenWidth - pixelX;
                pixelX = screenWidth;
            }
        } else {
            if (pixelX + 1 > 0) {
                newDeltaX += -1 - pixelX;
                pixelX = -1;
            } else if (pixelX + screenWidth < 0) {
                newDeltaX += -screenWidth - pixelX;
                pixelX = -screenWidth;
            }
        }
        
        if (pixelX >= 0 && pixelX + objWidth / 2 > screenWidth / 2) {
            pixelX -= screenWidth - objWidth;
        } else if (pixelX < 0 && pixelX + objWidth / 2 <= -screenWidth / 2) {
            pixelX += screenWidth - objWidth;
        }
        
        double newX;
        if (isRelative) {
            newX = pixelX / (double)screenWidth;
        } else {
            newX = pixelX;
        }
        
        setX(newX);
        
        return newDeltaX;
    }
    
    default int moveY(int deltaY, int objHeight) {
        double currentY = getY();
        int screenHeight = com.shyeuar.baity.gui.hud.HudScreenUtils.getScaledHeight();
        
        int pixelY;
        boolean isRelative = currentY >= 0 && currentY <= 1.0;
        if (isRelative) {
            pixelY = (int)(currentY * screenHeight);
        } else {
            pixelY = (int)currentY;
        }
        
        int wasPositiveY = pixelY >= 0 ? 1 : 0;
        pixelY += deltaY;
        
        int newDeltaY = deltaY;
        if (wasPositiveY == 1) {
            if (pixelY < 0) {
                newDeltaY -= pixelY;
                pixelY = 0;
            } else if (pixelY > screenHeight) {
                newDeltaY += screenHeight - pixelY;
                pixelY = screenHeight;
            }
        } else {
            if (pixelY + 1 > 0) {
                newDeltaY += -1 - pixelY;
                pixelY = -1;
            } else if (pixelY + screenHeight < 0) {
                newDeltaY += -screenHeight - pixelY;
                pixelY = -screenHeight;
            }
        }
        
        if (pixelY >= 0 && pixelY - objHeight / 2 > screenHeight / 2) {
            pixelY -= screenHeight - objHeight;
        } else if (pixelY < 0 && pixelY - objHeight / 2 <= -screenHeight / 2) {
            pixelY += screenHeight - objHeight;
        }
        
        double newY;
        if (isRelative) {
            newY = pixelY / (double)screenHeight;
        } else {
            newY = pixelY;
        }
        
        setY(newY);
        
        return newDeltaY;
    }
    
    void render(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTicks);
    
    boolean shouldRender();
}
