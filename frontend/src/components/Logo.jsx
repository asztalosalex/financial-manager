import logo from '../assets/logo.webp';
import '../assets/css/Logo.css';

const Logo = ({
  size = 'medium',
  className = '',
  onClick
}) => {
  const sizeClasses = {
    small: 'logo-small',
    medium: 'logo-medium',
    large: 'logo-large',
    xlarge: 'logo-xlarge'
  };

  const logoClasses = `logo ${sizeClasses[size]} ${className}`.trim();

  return (
    <div className={logoClasses} onClick={onClick}>
      <img 
        src={logo} 
        alt="Financial Manager Logo" 
        className="logo-image"
      />
    </div>
  );
};

export default Logo;
