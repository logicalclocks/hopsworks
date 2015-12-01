package se.kth.hopsworks.users;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@XmlRootElement
public class UserDTO {

  private String email;
  private String telephoneNum;
  private String firstName;
  private String lastName;
  private int status;
  private String securityQuestion;
  private String securityAnswer;
  private String secret;
  private String chosenPassword;
  private String repeatedPassword;
  private boolean ToS;
  private String orgName;
  private String dep;
  private String street;
  private String city;
  private String postCode;
  private String country;
  

  public UserDTO() {
  }

  public UserDTO(Users user) {
    this.email = user.getEmail();
    this.firstName = user.getFname();
    this.lastName = user.getLname();
    this.telephoneNum = user.getMobile();
    this.orgName= user.getOrganization().getOrgName();
    this.dep = user.getOrganization().getDepartment();
    this.street = user.getAddress().getAddress2();
    this.city = user.getAddress().getCity();
    this.postCode = user.getAddress().getPostalcode();
    this.country = user.getAddress().getCountry();
     
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTelephoneNum() {
    return telephoneNum;
  }

  public void setTelephoneNum(String telephoneNum) {
    this.telephoneNum = telephoneNum;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getSecurityQuestion() {
    return securityQuestion;
  }

  public void setSecurityQuestion(String securityQuestion) {
    this.securityQuestion = securityQuestion;
  }

  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getChosenPassword() {
    return chosenPassword;
  }

  public void setChosenPassword(String chosenPassword) {
    this.chosenPassword = chosenPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  public boolean getToS() {
    return ToS;
  }

  public void setToS(boolean ToS) {
    this.ToS = ToS;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getDep() {
    return dep;
  }

  public void setDep(String dep) {
    this.dep = dep;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  
  @Override
  public String toString() {
    return "UserDTO{" + "email=" + email + ", telephoneNum=" + telephoneNum
            + ", firstName=" + firstName + ", lastName=" + lastName
            + ", status=" + status + ", securityQuestion=" + securityQuestion
            + ", securityAnswer=" + securityAnswer + ", secret=" + secret
            + ", chosenPassword=" + chosenPassword + ", repeatedPassword="
            + repeatedPassword + ", ToS=" + ToS + ", orgName=" + orgName 
            + ", dep=" + dep+ ", street=" + street + ", city="+ city 
            + ", postCode= "+ postCode + ", country=" + country+
            '}';
  }

}
