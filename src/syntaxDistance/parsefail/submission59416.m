function [theta, J_history] = gradientDescent(X, y, theta, alpha, num_iters)
%GRADIENTDESCENT Performs gradient descent to learn theta
%   theta = GRADIENTDESENT(X, y, theta, alpha, num_iters) updates theta by 
%   taking num_iters gradient steps with learning rate alpha

% Initialize some useful values
m = length(y); % number of training examples
J_history = zeros(num_iters, 1);

    % ====================== YOUR CODE HERE ======================
    % Instructions: Perform a single gradient step on the parameter vector
    %               theta. 
    %
    % Hint: While debugging, it can be useful to print out the values
    %       of the cost function (computeCost) and gradient here.
    %


tempSum = [0;0];
for iter = 1:num_iters
	for i = 1:size(X, 1)
		tempSum(1) = tempSum(1) + (((X(i,:)) * theta) - y(i))*X(i,1);
		tempSum(2) = tempSum(2) + (((X(i,:)) * theta) - y(i))*X(i,2);
	endfor
	theta = theta - alpha * (1/size(X,1)) * tempSum;
endfor





    % ============================================================

    % Save the cost J in every iteration    
    J_history(iter) = computeCost(X, y, theta);

end

end
