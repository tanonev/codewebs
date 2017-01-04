function p = predict(Theta1, Theta2, X)
%PREDICT Predict the label of an input given a trained neural network
%   p = PREDICT(Theta1, Theta2, X) outputs the predicted label of X given the
%   trained weights of a neural network (Theta1, Theta2)

% Useful values
m = size(X, 1);
num_labels = size(Theta2, 1);

% You need to return the following variables correctly 
p = zeros(size(X, 1), 1);

% add bias
X = [ones(m, 1) X];

for j=1:m,

	z2 = sigmoid(X(j,:) * Theta1');

	z2 = [1 z2];
	[trash,p(j)] = max(sigmoid(z2 * Theta2'));
end;

% =========================================================================


end







% =========================================================================


end
