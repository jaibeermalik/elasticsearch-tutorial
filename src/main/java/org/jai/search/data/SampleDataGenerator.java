package org.jai.search.data;

import java.util.List;

import org.jai.search.model.Product;
import org.jai.search.model.ProductGroup;
import org.jai.search.model.ProductProperty;

public interface SampleDataGenerator
{
    public static final String COMPUTER = "Computer";
    public static final String RED = "Red";
    public static final String GREEN = "Green";
    public static final String BLUE = "Blue";
    public static final String COLORS = "Colors";
    public static final String AGE_18_PLUS_YEARS = "18 + years";
    public static final String AGE_0_12_YEARS = "0-12 years";
    public static final String AGE_12_18_YEARS = "12-18 years";
    public static final String AGE = "Age";
    public static final String BRANDS = "Brands";
    public static final String APPLE = "Apple";
    public static final String HP = "Apple";
    public static final String DELL = "Dell";
    public static final String LAPTOPS = "Laptops";
    public static final String MACBOOK = "Macbook";
    public static final String MACBOOK_PRO = "Macbook Pro";
    public static final String MACBOOK_AIR = "Macbook Air";
    public static final String CHROMEBOOK = "Chrmoebook";
    public static final String NETBOOK = "Netbook";
    
    
    public static final String PRODUCTPROPERTY_SIZE_12_INCH = "12 inch";
    public static final String PRODUCTPROPERTY_SIZE_13_INCH = "13 inch";
    public static final String PRODUCTPROPERTY_SIZE_15_INCH = "15 inch";
    public static final String PRODUCTPROPERTY_SIZE_17_INCH = "17 inch";
    public static final String PRODUCTPROPERTY_SIZE_21_INCH = "21 inch";
    
    public static final String PRODUCTPROPERTY_COLOR_BLACK = "Black";
    public static final String PRODUCTPROPERTY_COLOR_GREY = "Grey";
    public static final String PRODUCTPROPERTY_COLOR_YELLOW = "Yellow";
    public static final String PRODUCTPROPERTY_COLOR_PURPLE = "Purple";
    public static final String PRODUCTPROPERTY_COLOR_BROWN = "Brown";
    
    public static final String RESOLUTON_1024_600 = "1024 x 600";
    public static final String RESOLUTON_1024_758 = "1024 x 758";
    public static final String RESOLUTON_1920_1080 = "1920 x 1080";
    public static final String RESOLUTON_1920_1200 = "1920 x 1200";
    public static final String RESOLUTON_3200_1800 = "3200 x 1800";
    
    public static final String MEMORY_6144_MB = "6144 MB";
    public static final String MEMORY_2_GB = "2 GB";
    public static final String MEMORY_4_GB = "4 GB";
    public static final String MEMORY_6_GB = "6 GB";
    public static final String MEMORY_8_GB = "8 GB";
    
    List<Product> generateSampleData();

    List<ProductGroup> generateNestedDocumentsSampleData();

    ProductProperty findProductProperty(String size, String color);
}
