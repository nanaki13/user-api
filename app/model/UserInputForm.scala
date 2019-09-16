package model;


case class UserInputForm(email : String,login : String,  password : String )

object  UserInputForm{
  def apply (email : String,  password : String): UserInputForm = UserInputForm(email , "", password)
  def applyMailLogin (email : String,  login : String): UserInputForm = UserInputForm(email , login, "")
  def unapplyMailLogin (e : UserInputForm)= Some(e.email,e.login)
  def mailPasswordOption(e : UserInputForm ) = Some(e.email,e.password)
}