package mook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entry {
    private Integer id;
    private String author;
    private String text;
    private Date date;
    private List<Image> images;

    public Entry(Integer id, String author, String text, Date date, Image... images) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.date = date;
        this.images = new ArrayList<>();
        this.images.addAll(Arrays.asList(images));
    }
}