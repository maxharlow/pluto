object Start extends App {

  println("""
 ______   __       __  __   _________  ______   __       ________   _________  ______    __  __   
/_____/\ /_/\     /_/\/_/\ /________/\/_____/\ /_/\     /_______/\ /________/\/_____/\  /_/\/_/\  
\   _ \ \\ \ \    \ \ \ \ \\__    __\/\   _ \ \\ \ \    \    _  \ \\__    __\/\   _ \ \ \ \ \ \ \ 
 \ (_) \ \\ \ \    \ \ \ \ \  \  \ \   \ \ \ \ \\ \ \    \  (_)  \ \  \  \ \   \ (_) ) )_\ \_\ \ \
  \  ___\/ \ \ \____\ \ \ \ \  \  \ \   \ \ \ \ \\ \ \____\   __  \ \  \  \ \   \  __ `\ \\    _\/
   \ \ \    \ \/___/\\ \_\ \ \  \  \ \   \ \_\ \ \\ \/___/\\  \ \  \ \  \  \ \   \ \ `\ \ \ \  \ \
    \_\/     \_____\/ \_____\/   \__\/    \_____\/ \_____\/ \__\/\__\/   \__\/    \_\/ \_\/  \__\/
  """)

  Donations.run(Config.donationsData)
  Loans.run(Config.loansData)
  Companies.run()
  Members.run()

}
