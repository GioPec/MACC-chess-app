from flask import Flask,jsonify
from flask_restful import Resource, Api, reqparse
import random
import time
import multiprocessing
from flask import Flask
from stockfish import Stockfish
from flask import request

#stockfish = Stockfish("/home/JaR/mysite/stockfish_14.1_linux_x64_bmi2")

stock=[None] * 10

app = Flask(__name__)
api = Api(app)


{
    "Write Debug Log": "false",
    "Contempt": 0,
    "Min Split Depth": 0,
    "Threads": 1,
    "Ponder": "false",
    "Hash": 16,
    "MultiPV": 1,
    "Skill Level": 20,
    "Move Overhead": 30,
    "Minimum Thinking Time": 20,
    "Slow Mover": 80,
    "UCI_Chess960": "false",
}

parser = reqparse.RequestParser()



@app.route('/', methods=['GET'])
def mossa():
    index = request.args.get('index', type = int)
    return stock[index].get_best_move()

@app.route('/', methods=['POST'])
def domossa():
    index = request.args.get('index', type = int)
    move = request.args.get('move')
    mate = "false"
    valid = stock[index].is_move_correct(move)


    if(valid):
        stock[index].make_moves_from_current_position([move])
        stockfish_best_move = stock[index].get_best_move()

        if(stockfish_best_move == None):
            mate = "true"

    return {'valid': valid, 'mate': mate}



@app.route('/info', methods=['GET'])
def infoget():
    index = request.args.get('index', type = int)
    return stock[index].get_evaluation()

@app.route('/info', methods=['POST'])
def infopost():
    index = request.args.get('index', type = int)
    elo = request.args.get('ELO')
    stock[index].set_elo_rating(elo)
    return {'ELO': elo}


@app.route('/hello', methods=['GET'])
def hello():
    stato="OK"
    return jsonify({'state':stato})


@app.route('/reset', methods=['GET'])
def reset():
    index = request.args.get('index', type = int)
    errore=False
    # stock[index].set_fen_position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    if(index>=0 and index<=9):
        stock[index] = None
    else:
        errore=True

    return jsonify({'reset_id': index, 'errore': errore})

@app.route('/fen', methods=['GET'])
def fen():
    index = request.args.get('index', type = int)
    return {'fen': stock[index].get_fen_position(),'state': stock[index].get_board_visual()}


@app.route('/bestmove', methods=['GET'])
def bestmove():
    index = request.args.get('index', type = int)
    return {'move': stock[index].get_best_move() }





@app.route('/stockfish', methods=['POST'])
def stockfish():
    index = request.args.get('index', type = int)
    move = request.args.get('move')
    mate = ""
    stockfish_best_move = ""
    valid = stock[index].is_move_correct(move)
    if(valid):
        stock[index].make_moves_from_current_position([move])
        stockfish_best_move = stock[index].get_best_move()
        if(stockfish_best_move != None):
            stock[index].make_moves_from_current_position([stockfish_best_move])
            player_best_move = stock[index].get_best_move()
            if(player_best_move == None):
                mate = "stockfish"
        else:
            mate = "player"

    return {'valid': valid, 'response': stockfish_best_move, 'mate': mate}


@app.route('/matchId', methods=['GET'])
def matchId():
    global stock
    if (None in stock):
        indici=[i for i,v in enumerate(stock) if v == None]
        print(indici)
        return indici[0]
    else:
        return 404

@app.route('/startMatch', methods=['GET'])
def startMatch():
    global stock
    if (None in stock):
        indici=[i for i,v in enumerate(stock) if v == None]
        index=indici[0]
        stock[index] = Stockfish("/home/JaR/mysite/stockfish_14.1_linux_x64_bmi2")
        stock[index].set_fen_position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        print("started")
        return jsonify({'response':index})
    else:
        return jsonify({'response':404})




if __name__ == '__main__':
    print('starting myHUB api...waiting')