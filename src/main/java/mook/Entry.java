package mook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public record Entry(Integer id, String author, String text, Date date, List<Image> images) {

    public Entry(Integer id, String author, String text, Date date) {
        this(id, author, text, date, new ArrayList<>());
    }
}