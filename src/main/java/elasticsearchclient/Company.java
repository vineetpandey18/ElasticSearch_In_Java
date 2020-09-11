package elasticsearchclient;

import java.util.Date;

public class Company {

    private int age;

    private String companyName;

    private Date dateOfEstd;

    public Company() {
    }

    public Company(int age, String companyName, Date dateOfEstd) {
        super();
        this.age = age;
        this.companyName = companyName;
        this.dateOfEstd = dateOfEstd;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Date getDateOfEstd() {
        return dateOfEstd;
    }

    public void setDateOfEstd(Date dateOfEstd) {
        this.dateOfEstd = dateOfEstd;
    }

    @Override
    public String toString() {
        return "Company [age=" + age + ", companyName=" + companyName + ", dateOfEstd=" + dateOfEstd + "]";
    }
}