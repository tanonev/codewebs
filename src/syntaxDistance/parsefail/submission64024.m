function [theta, J_history] = gradientDescent(X, y, theta, alpha, num_iters)
%GRADIENTDESCENT Performs gradient descent to learn theta
%   theta = GRADIENTDESENT(X, y, theta, alpha, num_iters) updates theta by 
%   taking num_iters gradient steps with learning rate alpha

% Initialize some useful values
m = length(y); % number of training examples
J_history = zeros(num_iters, 1);
% fprintf('size of theta=%d %f %f \n',size(theta), theta(1), theta(2));

%XX = featureNormalize(X);
% XX = X;

% for iter = 1:num_iters
    % ====================== YOUR CODE HERE ======================
    % Instructions: Perform a single gradient step on the parameter vector
    %               theta. 
    %
    % Hint: While debugging, it can be useful to print out the values
    %       of the cost function (computeCost) and gradient here.
    %

	%t = theta - alpha * pinv(XX') * y;
	theta = pinv(X'*X) * X'*y;
% fprintf('%f %f %f \n',J_history(iter) , theta(1), theta(2));
%	for ii = 1:m
%		t1 = theta(1) - alpha*(((theta(1) + theta(2)*XX(ii,2)) - y(ii))); 
%		t2 = theta(2) - alpha*(((theta(1) + theta(2)*XX(ii,2))  - y(ii)))*XX(ii,2);
% if(t1 < theta(1) && t1 > 0)
%	theta(1) = t1;
% end
% if(t2 < theta(2) && t2 > 0)
%	theta(2) = t2;
% end 
 % fprintf('C %d %f %f \n',ii, theta(1), theta(2));
%	end

    %J_history(iter) = computeCost(XX, y, theta);
%fprintf('J(%d)=%f t0=%f t1=%f \n',iter,J_history(iter) , theta(1), theta(2));




    % ============================================================

    % Save the cost J in every iteration    
    % J_history(iter) = computeCost(X, y, theta);
% until((++iter >= num_iters) || (J_history(iter-1) <= alpha));
end
% fprintf('J=%f t0=%f t1=%f \n',J_history(iter-1) , theta(1), theta(2));
% X = XX;

end
