import json
import os


class User:
    def __init__(self, username, password, email):
        self.username = username
        self.password = password
        self.email = email


class UserRepository:
    def __init__(self):
        self.users = []
        self.isLoggedIn = False
        self.currentUser = None
        self.loadUser()

    def loadUser(self):
        if os.path.exists('users.json'):
            with open('users.json', 'r', encoding='utf-8') as file:
                try:
                    users = json.load(file)
                    for user in users:
                        loaded_user = User(username=user['username'], password=user['password'], email=user['email'])
                        self.users.append(loaded_user)
                except json.JSONDecodeError as e:
                    print("JSON decoding error:", e)

    def register(self, user: User):
        self.users.append(user)
        self.saveToFile()
        print('Kullanıcı oluşturuldu')

    def login(self, username, password):
        for user in self.users:
            if user.username == username and user.password == password:
                self.isLoggedIn = True
                self.currentUser = user
                print(f'{username} kullanıcısı giriş yaptı.')
                return
        print('Kullanıcı adı veya şifre hatalı.')

    def logout(self):
        if self.isLoggedIn:
            self.isLoggedIn = False
            self.currentUser = None
            print('Çıkış yapıldı.')
        else:
            print('Henüz giriş yapılmamış.')

    def identity(self):
        if self.isLoggedIn:
            print('User İdentity'.center(50, '*'))
            print(f'Username: {self.currentUser.username}')
            print(f'Password: {self.currentUser.password}')
            print(f'Email: {self.currentUser.email}')
            print(''.center(50, '*'))
        else:
            print('Henüz giriş yapılmamış.')

    def saveToFile(self):
        with open('users.json', 'w', encoding='utf-8') as file:
            user_dicts = [vars(user) for user in self.users]
            json.dump(user_dicts, file)


repository = UserRepository()

while True:
    if repository.isLoggedIn:
        print('Menü'.center(50, '*'))
        secim = input('1- İdentity\n2- Logout\n3- Exit\n')
        if secim == '3':
            break
        elif secim == '1':
            repository.identity()
        elif secim == '2':
            repository.logout()
    else:
        print('Menü'.center(50, '*'))
        secim = input('1- Register\n2- Login\n3- Exit\n')
        if secim == '3':
            break
        elif secim == '1':
            username = input('Username: ')
            password = input('Password: ')
            email = input('Email: ')
            user = User(username=username, password=password, email=email)
            repository.register(user)
        elif secim == '2':
            username = input('Username: ')
            password = input('Password: ')
            repository.login(username, password)
