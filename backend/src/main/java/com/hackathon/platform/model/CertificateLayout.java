package com.hackathon.platform.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateLayout {
    private String pageSize = "A4-landscape";
    private List<CertificateElement> elements = new ArrayList();

    @Getter
    @Setter
    public static class CertificateElement{

        private String type;
        private String field;
        private String staticText;
        private String imageStorageKey;
        private String label;
        private double width = 150;
        private double height = 80;
        private double x;
        private double y;
        private String font = "Helvetica";
        private double fontSize = 16;
        private String color = "#00000";
        private String align = "left";
        private String visibleForTypes;

        public boolean isVisibleFor(String certificateType){
            if(visibleForTypes == null || visibleForTypes.isBlank()){
                return true;
            }
            for(String allowed : visibleForTypes.split(",")){
                if(allowed.trim().equalsIgnoreCase(certificateType)){
                    return true;
                }
            }
            return false;
        }
    }
}