import React from 'react';

/**
 * Error Boundary - 捕获子组件错误，防止整个应用崩溃
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
    this.setState({ errorInfo });
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          padding: '24px',
          margin: '16px',
          borderRadius: '12px',
          background: 'linear-gradient(135deg, #fff5f5 0%, #fed7d7 100%)',
          border: '1px solid #fc8181',
          textAlign: 'center',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
          <h3 style={{ color: '#c53030', margin: '0 0 8px' }}>
            {this.props.title || '组件加载出错'}
          </h3>
          <p style={{ color: '#742a2a', fontSize: '14px', margin: '0 0 16px' }}>
            {this.state.error?.message || '发生了未知错误'}
          </p>
          <button
            onClick={this.handleRetry}
            style={{
              padding: '8px 24px',
              borderRadius: '8px',
              border: 'none',
              background: '#e53e3e',
              color: 'white',
              cursor: 'pointer',
              fontSize: '14px',
            }}
          >
            重试
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
