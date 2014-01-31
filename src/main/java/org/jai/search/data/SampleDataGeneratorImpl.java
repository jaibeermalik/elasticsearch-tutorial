package org.jai.search.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.joda.time.DateTime;
import org.jai.search.model.Category;
import org.jai.search.model.Product;
import org.jai.search.model.ProductGroup;
import org.jai.search.model.ProductProperty;
import org.jai.search.model.SearchFacetName;
import org.jai.search.model.Specification;
import org.springframework.stereotype.Service;

@Service
public class SampleDataGeneratorImpl implements SampleDataGenerator
{
    @Override
    public List<Product> generateSampleData()
    {
        Set<Category> hierarchicalCategories = generateHierarchicalCategoryData();
        Set<ProductProperty> productProperties = getProductProperties();
        
        List<Product> products = new ArrayList<Product>();
        for (int i = 0; i < 50; i++)
        {
            Product product = new Product();
            product.setId(Long.valueOf(i));
            product.setTitle("Title " + i);
            product.setDescription("Description" + i);
            product.setAvailableOn(new DateTime().plusDays(i).toDate());
            product.addKeyword("Keyword " + i);
            product.setPrice(BigDecimal.valueOf(i));
            product.setSoldOut(i % 2 == 0 ? true : false);
            product.setBoostFactor(i/10000f);
            
            if(i <5)
            {
                product.addCategory(findCategory(hierarchicalCategories, MACBOOK_AIR));
                product.addCategory(findCategory(hierarchicalCategories, APPLE));
                product.addCategory(findCategory(hierarchicalCategories, RED));
                product.addCategory(findCategory(hierarchicalCategories, AGE_18_PLUS_YEARS));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_21_INCH, PRODUCTPROPERTY_COLOR_BROWN));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_17_INCH, PRODUCTPROPERTY_COLOR_PURPLE));
                
                product.addSpecification(new Specification(RESOLUTON_3200_1800, MEMORY_8_GB));
                product.addSpecification(new Specification(RESOLUTON_1920_1200, MEMORY_6_GB));
            }
            else if(i >=5 && i<10)
            {
                product.addCategory(findCategory(hierarchicalCategories, MACBOOK_PRO));
                product.addCategory(findCategory(hierarchicalCategories, APPLE));
                product.addCategory(findCategory(hierarchicalCategories, BLUE));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_15_INCH, PRODUCTPROPERTY_COLOR_YELLOW));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_17_INCH, PRODUCTPROPERTY_COLOR_PURPLE));
                
                product.addSpecification(new Specification(RESOLUTON_1920_1080, MEMORY_6_GB));
                product.addSpecification(new Specification(RESOLUTON_1920_1200, MEMORY_6_GB));
            }
            else if(i >=10 && i<20)
            {
                product.addCategory(findCategory(hierarchicalCategories, HP));
                product.addCategory(findCategory(hierarchicalCategories, AGE_12_18_YEARS));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_12_INCH, PRODUCTPROPERTY_COLOR_BLACK));
                
                product.addSpecification(new Specification(RESOLUTON_1920_1080, MEMORY_4_GB));
                product.addSpecification(new Specification(RESOLUTON_1920_1080, MEMORY_2_GB));
            }
            else
            {
                product.addCategory(findCategory(hierarchicalCategories, DELL));
                product.addCategory(findCategory(hierarchicalCategories, GREEN));
                product.addCategory(findCategory(hierarchicalCategories, AGE_0_12_YEARS));
                product.addProductProperty(findProductProperty(productProperties, PRODUCTPROPERTY_SIZE_13_INCH, PRODUCTPROPERTY_COLOR_GREY));
                
                product.addSpecification(new Specification(RESOLUTON_1024_758, MEMORY_2_GB));
                product.addSpecification(new Specification(RESOLUTON_1024_600, MEMORY_6144_MB));
            }
            products.add(product);
        }
        
        return products;
    }
    
    private Set<ProductProperty> getProductProperties()
    {
        Set<ProductProperty> productProperties = new HashSet<ProductProperty>();
        
        String[] sizes = new String[] {PRODUCTPROPERTY_SIZE_12_INCH,
                PRODUCTPROPERTY_SIZE_13_INCH,
                PRODUCTPROPERTY_SIZE_15_INCH,
                PRODUCTPROPERTY_SIZE_17_INCH,
                PRODUCTPROPERTY_SIZE_21_INCH
        };

        String[] colors = new String[] {PRODUCTPROPERTY_COLOR_BLACK,
                PRODUCTPROPERTY_COLOR_GREY,
                PRODUCTPROPERTY_COLOR_YELLOW,
                PRODUCTPROPERTY_COLOR_PURPLE,
                PRODUCTPROPERTY_COLOR_BROWN
                                        }; 
        for (int i = 0, j=0; i < 10; i++)
        {
            ProductProperty productProperty = new ProductProperty();
            productProperty.setId(Long.valueOf(i));
            if(i<5)
            {
                productProperty.setSize(sizes[i]);
                productProperty.setColor(colors[i]);
            }
            else
            {
                productProperty.setSize(sizes[j++]);
                productProperty.setColor(colors[9-i]);
            }
            productProperties.add(productProperty);
        }
        System.out.println(productProperties);
        return productProperties;
    }
    
    private ProductProperty findProductProperty(Set<ProductProperty> productProperties, String size, String color)
    {
        for (ProductProperty productProperty : productProperties)
        {
            if(size.equals(productProperty.getSize()) && color.equals(productProperty.getColor()))
            {
                return productProperty;
            }
        }
        return null;
    }
    
    @Override
    public ProductProperty findProductProperty(String size, String color)
    {
        return findProductProperty(getProductProperties(), size, color);
    }
    
    @Override
    public List<ProductGroup> generateNestedDocumentsSampleData()
    {
        List<ProductGroup> productGroups = new ArrayList<ProductGroup>();
        
        List<Product> sampleData = generateSampleData();
        int count = 0;
        for (int i = 1; i <= 10; i++)
        {
            ProductGroup productGroup = new ProductGroup();
            productGroup.setId(Long.valueOf(i));
            productGroup.setGroupTitle("groupTitle" + i);
            productGroup.setGroupDescription("groupDescription" + i );
            
            while(count < i*5)
            {
                Product product = sampleData.get(count);
                productGroup.addProduct(product);
                count = count + 1;
            }
            productGroups.add(productGroup);
        }
        return productGroups;
    }
    
    private Set<Category> generateHierarchicalCategoryData()
    {
        Category computerCategory = new Category(COMPUTER, null, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category laptops = new Category(LAPTOPS, computerCategory, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category macbookLaptops = new Category(MACBOOK, laptops, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category macbookProLaptops = new Category(MACBOOK_PRO, macbookLaptops, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category macbookAirLaptops = new Category(MACBOOK_AIR, macbookLaptops, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category chrmoebookLaptops = new Category(CHROMEBOOK, laptops, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        Category netbookLaptops = new Category(NETBOOK, laptops, SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getCode());
        
        Category brands = new Category(BRANDS, null, SearchFacetName.SEARCH_FACET_TYPE_BRAND.getCode());
        Category appleBrand = new Category(APPLE, brands, SearchFacetName.SEARCH_FACET_TYPE_BRAND.getCode());
        Category hpBrand = new Category(HP, brands, SearchFacetName.SEARCH_FACET_TYPE_BRAND.getCode());
        Category dellBrand = new Category(DELL, brands, SearchFacetName.SEARCH_FACET_TYPE_BRAND.getCode());
        
        Category age = new Category(AGE, null, SearchFacetName.SEARCH_FACET_TYPE_AGE.getCode());
        Category kidAge = new Category(AGE_0_12_YEARS, age, SearchFacetName.SEARCH_FACET_TYPE_AGE.getCode());
        Category teenAge = new Category(AGE_12_18_YEARS, age, SearchFacetName.SEARCH_FACET_TYPE_AGE.getCode());
        Category adultAge = new Category(AGE_18_PLUS_YEARS, age, SearchFacetName.SEARCH_FACET_TYPE_AGE.getCode());
        
        Category colors = new Category(COLORS, null, SearchFacetName.SEARCH_FACET_TYPE_COLOR.getCode());
        Category redColor = new Category(RED, colors, SearchFacetName.SEARCH_FACET_TYPE_COLOR.getCode());
        Category greenColor = new Category(GREEN, colors, SearchFacetName.SEARCH_FACET_TYPE_COLOR.getCode());
        Category blueColor = new Category(BLUE, colors, SearchFacetName.SEARCH_FACET_TYPE_COLOR.getCode());
        
        Set<Category> categories = new HashSet<Category>();
        categories.add(computerCategory);
        categories.add(laptops);
        categories.add(macbookLaptops);
        categories.add(macbookProLaptops);
        categories.add(macbookAirLaptops);
        categories.add(chrmoebookLaptops);
        categories.add(netbookLaptops);
        
        categories.add(brands);
        categories.add(appleBrand);
        categories.add(hpBrand);
        categories.add(dellBrand);

        categories.add(age);
        categories.add(kidAge);
        categories.add(teenAge);
        categories.add(adultAge);
        
        categories.add(colors);
        categories.add(redColor);
        categories.add(greenColor);
        categories.add(blueColor);
        
        return categories;
    }
    
    private Category findCategory(Set<Category> categories, String catName)
    {
        for (Category category : categories)
        {
            if(category.getName().equals(catName)) return category;
        }
        return null;
    }
    
    
}
