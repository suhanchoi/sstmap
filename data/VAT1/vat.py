import contextlib
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim

@contextlib.contextmanager
def _disable_tracking_bn_stats(model):
    def switch_attr(m):
        if hasattr(m, 'track_running_stats'):
            m.track_running_stats ^= True
    model.apply(switch_attr)
    yield
    model.apply(switch_attr)
    
def normalize(d):
    d /= (torch.sqrt(torch.sum(d**2, axis=1)).view(-1,1)+1e-16)
    return d

def _l2_normalize(d):
    d_reshaped = d.view(d.shape[0], -1, *(1 for _ in range(d.dim() - 2)))
    d /= torch.norm(d_reshaped, dim=1, keepdim=True) + 1e-16
    return d

def _kl_div(p,q):
    '''
    D_KL(p||q) = Sum(p log p - p log q)
    '''
    logp = torch.nn.functional.log_softmax(p,dim=1)
    logq = torch.nn.functional.log_softmax(q,dim=1)
    p = torch.exp(logp)
    return (p*(logp-logq)).sum(dim=1,keepdim=True).mean()

class VATLoss(nn.Module):
    def __init__(self, xi =0.001, eps=0.1, ip=2):
        """
        VAT loss
        :param xi: hyperparameter of VAT.  a small float for the approx. of the finite difference method.
        :param eps: hyperparameter of VAT. the value for how much deviate from original data point X.
        :param ip: a number of power iteration for approximation of r_vadv. The default value 2 is sufficient.
        """
        super(VATLoss, self).__init__()
        self.xi = xi
        self.eps = eps
        self.ip = ip
        
    def forward(self, model, x):
        with torch.no_grad():
            pred = model(x)
        
        # random unit for perturbation
        d = torch.randn(x.shape)
        d = _l2_normalize(d)
        for _ in range(self.ip):
            d.requires_grad_()
            pred_hat = model(x + self.xi * d)
            adv_distance = _kl_div(pred_hat, pred)
            adv_distance.backward()
            d = _l2_normalize(d.grad.data)
            model.zero_grad()
        
        r_adv = d*self.eps
        pred_hat = model(x+r_adv)
        lds = _kl_div(pred_hat, pred)
        return lds
