package unit731.hunspeller.gui.tags;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.JavaHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class JTagPanel extends JPanel{

	private final Object synchronizer = new Object();


	public JTagPanel(){
		setLayout(new FlowLayout(FlowLayout.LEADING, 2, 4));
//		setLayout(new GridBagLayout());
//		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
//		add(Box.createRigidArea(new Dimension(4, 0)));

//		add(new JTagComponent("a", this::removeTag));
//		add(new JTagComponent("b", this::removeTag));

		final JTextArea t = new JTextArea();
		t.setLineWrap(true);
		t.setWrapStyleWord(true);
		final Dimension ps = t.getPreferredSize();
		t.setPreferredSize(new Dimension(ps.width, ps.height * 2 * 16 / 10));
		t.setBorder(null);
		t.setOpaque(false);
		final JTagPanel parent = this;
		t.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent evt){
				final String text = t.getText();
				if(StringUtils.isNotBlank(text)){
					synchronized(synchronizer){
						final JTagComponent tag = new JTagComponent(text.trim(), parent::removeTag);
//tag.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
						parent.add(tag, parent.getComponentCount() - 1);
//						parent.add(Box.createRigidArea(new Dimension(4, 0)), parent.getComponentCount() - 1);

						//reset input
						t.setText(StringUtils.EMPTY);

						//force repaint of the component
						repaint();
						revalidate();
					}
				}
			}
		});

		//FIXME
		setPreferredSize(new Dimension(400, ps.height * 2 * 16 / 10));
		setBackground(UIManager.getColor("TextArea.background"));

		add(t);
	}

	private void removeTag(final JTagComponent tag){
		remove(tag);

		repaint();
		revalidate();
	}

	public Set<String> getTags(){
		synchronized(synchronizer){
			return JavaHelper.nullableToStream(getComponents())
				.filter(comp -> comp instanceof JTagComponent)
				.flatMap(comp -> Arrays.stream(((JTagComponent)comp).getComponents()))
				.filter(comp -> comp instanceof JLabel)
				.map(comp -> ((JLabel)comp).getText())
				.collect(Collectors.toSet());
		}
	}


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){}

		class JTagPanelExample extends JFrame{
			public JTagPanelExample(){
				setSize(new Dimension(500, 180));
				GridBagLayout layout = new GridBagLayout();
				GridBagConstraints gbc = new GridBagConstraints();
				setLayout(layout);
				gbc.gridx = 0;
				gbc.gridy = 0;

				JPanel panel = new JTagPanel();
				add(panel, gbc);
				setVisible(true);
			}
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new JTagPanelExample()).setVisible(true));
	}

}
