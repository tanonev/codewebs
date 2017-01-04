class FeedbackGenerator(object):

    def __init__(self):
        raise Exception('can\'t initialize abstract type')

    def getFeedback(self, ast, code, charmap, assn):
        raise Exception('called abstract method')       
